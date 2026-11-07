#!/usr/bin/env node
// [진행자용] 결과 집계. 참가자마다 mcp(실험군)·nomcp(대조군) 두 실행을 짝지어 비교한다.
// 입력: docs/record-sheet.csv (진행자 기록), submissions/<ID>/<run>/ (collect 산출물 또는 gather-submissions.sh 로 모은 것)
// 출력: submissions/report.md
//
//   npm run aggregate [-- <record-sheet.csv> <submissions>]
//
// 실행 시작 시각 = 기록 시트의 mcpStart/nomcpStart. 비어 있으면 그 실행의 첫 verify 기록(시작 전 실행) 의 startedAt.
// 마일스톤 도달 시각 = 그 항목을 처음 통과한 verify 실행의 종료 시각 − 시작 시각 (verify 1회 2~3분 지연, 두 실행 동일).
import fs from "node:fs";
import path from "node:path";
import { KIT_DIR, SUBMISSIONS_DIR, RUNS, RUN_MINUTES } from "../config/kit.config.mjs";
import { MILESTONES, MILESTONE_IDS, GROUPS } from "../config/milestones.mjs";
import { median, fmt, wilcoxon } from "./lib/stats.mjs";

const sheetPath = process.argv[2] ?? path.join(KIT_DIR, "docs", "record-sheet.csv");
const root = process.argv[3] ?? SUBMISSIONS_DIR;
const MS = MILESTONE_IDS;
const MAX = MS.length;

const [header, ...rows] = fs.readFileSync(sheetPath, "utf8").trim().split("\n").map((l) => l.split(","));
const people = rows
  .filter((r) => r.length >= 2 && r[0] && !r[0].startsWith("("))
  .map((r) => Object.fromEntries(header.map((h, i) => [h.trim(), (r[i] ?? "").trim()])));
const readJsonl = (f) => (fs.existsSync(f) ? fs.readFileSync(f, "utf8").split("\n").filter(Boolean).map((l) => JSON.parse(l)) : []);
const readJson = (f) => (fs.existsSync(f) ? JSON.parse(fs.readFileSync(f, "utf8")) : undefined);

// Claude Code 트랜스크립트에서 사람이 직접 입력한 프롬프트만 센다 (다른 도구는 미지원 → undefined).
function countPrompts(dir, tool) {
  if (tool !== "claude" || !fs.existsSync(dir)) return undefined;
  let n = 0;
  for (const f of fs.readdirSync(dir).filter((x) => x.endsWith(".jsonl")))
    for (const ev of readJsonl(path.join(dir, f))) {
      if (ev.type !== "user" || ev.isMeta || ev.isSidechain) continue;
      const c = ev.message?.content;
      const text = typeof c === "string" ? c : Array.isArray(c) && !c.some((b) => b.type === "tool_result") ? c.map((b) => b.text ?? "").join("") : "";
      if (text.trim() && !text.startsWith("<command-") && !text.startsWith("<local-command")) n++;
    }
  return n;
}

function loadRun(p, run) {
  const dir = path.join(root, p.participantId, run);
  const progress = readJsonl(path.join(dir, "progress.jsonl"));
  const fin = readJson(path.join(dir, "final.json"));
  if (fin) progress.push(fin);
  progress.sort((a, b) => a.finishedAt.localeCompare(b.finishedAt));
  const sheetStart = p[`${run}Start`] ? new Date(p[`${run}Start`].replace(" ", "T")).getTime() : NaN;
  const start = Number.isNaN(sheetStart) ? (progress[0] ? new Date(progress[0].startedAt).getTime() : NaN) : sheetStart;
  const reach = {};
  for (const r of progress) {
    const t = (new Date(r.finishedAt).getTime() - start) / 60000;
    for (const m of MS) if (r.milestones?.[m]?.pass && reach[m] === undefined) reach[m] = Math.max(0, t);
  }
  const last = progress[progress.length - 1];
  const size = readJson(path.join(dir, "size.json"));
  return { run, exists: progress.length > 0, score: last?.score ?? 0, groups: last?.groups ?? {}, rate: last?.complianceRate, reach, verifyRuns: progress.length, lines: size?.sourceLines, prompts: countPrompts(path.join(dir, "transcripts"), p.aiTool) };
}
const data = people.map((p) => {
  const meta = readJson(path.join(root, p.participantId, "meta.json")) ?? {};
  const aiTool = p.aiTool || meta.tool || "-";
  const order = p.order || meta.order || "-";
  const runs = Object.fromEntries(RUNS.map((r) => [r, loadRun({ ...p, aiTool }, r)]));
  return { ...p, aiTool, order, runs };
});

const both = data.filter((d) => d.runs.mcp.exists && d.runs.nomcp.exists);
const L = [];
L.push("# 생산성 측정 결과 (실험군 mcp: codegen MCP 연결 / 대조군 nomcp: 미연결)", "");
L.push(`- 참가자 ${data.length}명 (두 실행 모두 제출 ${both.length}명) · 제한 시간 mcp ${RUN_MINUTES.mcp}분 / nomcp ${RUN_MINUTES.nomcp}분 · 만점 ${MAX}`, "");

L.push("## 1. 실행별 요약", "");
L.push(`| 실행 | n | 만점 | 점수 중앙값 | ${GROUPS.map((g) => `${g}`).join(" | ")} | 적용률 중앙값 | 소스 라인 중앙값 | 프롬프트 중앙값 |`);
L.push(`|---|---|---|---|${GROUPS.map(() => "---").join("|")}|---|---|---|`);
for (const run of RUNS) {
  const rs = data.map((d) => d.runs[run]).filter((r) => r.exists);
  L.push(`| ${run} | ${rs.length} | ${rs.filter((r) => r.score === MAX).length} | ${fmt(median(rs.map((r) => r.score)))} | ${GROUPS.map((g) => fmt(median(rs.map((r) => r.groups?.[g]?.pass)))).join(" | ")} | ${fmt(median(rs.map((r) => r.rate)), 0)}% | ${fmt(median(rs.map((r) => r.lines)), 0)} | ${fmt(median(rs.map((r) => r.prompts)), 0)} |`);
}

L.push("", "## 2. 쌍 비교 (같은 사람의 mcp − nomcp)", "");
const diffs = both.map((d) => d.runs.mcp.score - d.runs.nomcp.score);
const w = wilcoxon(both.map((d) => [d.runs.mcp.score, d.runs.nomcp.score]));
L.push(`- 점수 차 중앙값: ${fmt(median(diffs))} (mcp 가 높은 사람 ${w?.positive ?? "-"}명, nomcp 가 높은 사람 ${w?.negative ?? "-"}명, 같음 ${both.length - (w?.positive ?? 0) - (w?.negative ?? 0)}명)`);
if (w) L.push(`- Wilcoxon 부호 순위 검정: W+ = ${fmt(w.wPlus, 0)}, p = ${fmt(w.p, 3)} (참고용)`);
L.push(`- 주의: mcp 는 ${RUN_MINUTES.mcp}분, nomcp 는 ${RUN_MINUTES.nomcp}분으로 시간이 다르다. 같은 시점(예: 20분)의 점수를 비교하려면 3번 표를 본다.`);

L.push("", "## 3. 시간에 따른 평균 진척 점수 (도달 곡선)", "");
const ticks = [5, 10, 15, 20, 25, 30];
L.push(`| 실행 | ${ticks.map((t) => `${t}분`).join(" | ")} |`);
L.push(`|---|${ticks.map(() => "---").join("|")}|`);
for (const run of RUNS) {
  const rs = data.map((d) => d.runs[run]).filter((r) => r.exists);
  L.push(`| ${run} | ${ticks.map((t) => (t > RUN_MINUTES[run] ? "·" : rs.length ? fmt(rs.map((r) => MS.filter((m) => r.reach[m] != null && r.reach[m] <= t).length).reduce((a, b) => a + b, 0) / rs.length) : "-")).join(" | ")} |`);
}

L.push("", "## 4. 마일스톤 도달 (도달 인원 / 도달 시간 중앙값 분)", "");
L.push(`| 실행 | ${MS.join(" | ")} |`);
L.push(`|---|${MS.map(() => "---").join("|")}|`);
for (const run of RUNS) {
  const rs = data.map((d) => d.runs[run]).filter((r) => r.exists);
  L.push(`| ${run} | ${MS.map((m) => {
    const ts = rs.map((r) => r.reach[m]).filter((x) => x != null);
    return `${ts.length}/${rs.length} · ${fmt(median(ts))}`;
  }).join(" | ")} |`);
}

L.push("", "## 5. 순서 효과 (먼저 한 실행 vs 나중에 한 실행)", "");
L.push("| 순서 | n | mcp 점수 중앙값 | nomcp 점수 중앙값 |", "|---|---|---|---|");
for (const o of ["mcp-first", "nomcp-first"]) {
  const ds = both.filter((d) => d.order === o);
  L.push(`| ${o} | ${ds.length} | ${fmt(median(ds.map((d) => d.runs.mcp.score)))} | ${fmt(median(ds.map((d) => d.runs.nomcp.score)))} |`);
}

L.push("", "## 6. AI 도구별 (참고 — 도구는 무작위 배정이 아니므로 인과 해석 금지)", "");
L.push("| 도구 | n | mcp 점수 중앙값 | nomcp 점수 중앙값 | 차 중앙값 |", "|---|---|---|---|---|");
for (const tool of [...new Set(both.map((d) => d.aiTool))].sort()) {
  const ds = both.filter((d) => d.aiTool === tool);
  L.push(`| ${tool} | ${ds.length} | ${fmt(median(ds.map((d) => d.runs.mcp.score)))} | ${fmt(median(ds.map((d) => d.runs.nomcp.score)))} | ${fmt(median(ds.map((d) => d.runs.mcp.score - d.runs.nomcp.score)))} |`);
}

L.push("", "## 7. 참가자별", "");
L.push(`| ID | 도구 | 모델 | 순서 | eGov 경력 | AI 숙련도 | mcp 점수 | nomcp 점수 | mcp 적용률 | nomcp 적용률 | mcp verify | nomcp verify |`);
L.push(`|---|---|---|---|---|---|---|---|---|---|---|---|`);
for (const d of data)
  L.push(`| ${d.participantId} | ${d.aiTool} | ${d.model || "-"} | ${d.order} | ${d.egovYears ?? "-"} | ${d.aiToolSkill ?? "-"} | ${d.runs.mcp.exists ? d.runs.mcp.score : "-"} | ${d.runs.nomcp.exists ? d.runs.nomcp.score : "-"} | ${fmt(d.runs.mcp.rate, 0)}% | ${fmt(d.runs.nomcp.rate, 0)}% | ${d.runs.mcp.verifyRuns} | ${d.runs.nomcp.verifyRuns} |`);

L.push("", "## 8. 참가자·실행별 마일스톤 도달 시각(분)", "");
L.push(`| ID | 실행 | ${MS.join(" | ")} |`);
L.push(`|---|---|${MS.map(() => "---").join("|")}|`);
for (const d of data) for (const run of RUNS) if (d.runs[run].exists) L.push(`| ${d.participantId} | ${run} | ${MS.map((m) => fmt(d.runs[run].reach[m])).join(" | ")} |`);

L.push("", "## 마일스톤 범례", "");
for (const [id, g, name] of MILESTONES) L.push(`- ${id} (${g}) ${name}`);
L.push("", "항목별 가중치를 적용한 점수는 센터에서 별도로 계산한다. 이 표의 점수는 통과 항목 수(동일 가중)이다.");

fs.mkdirSync(root, { recursive: true });
fs.writeFileSync(path.join(root, "report.md"), L.join("\n") + "\n");
console.log(`집계 완료: ${path.join(root, "report.md")} (${data.length}명, 두 실행 모두 ${both.length}명)`);
