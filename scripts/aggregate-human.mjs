#!/usr/bin/env node
// [진행자용] 사람 테스트 결과 집계.
// 입력: docs/record-sheet.csv (진행자 기록), results/human/<ID>/ (collect 산출물 또는 gather-submissions.sh 로 모은 것)
// 출력: results/human/report.md
//
//   npm run aggregate [-- <record-sheet.csv> <results/human>]
//
// 마일스톤 도달 시각 = 그 마일스톤을 처음 통과한 verify 실행의 종료 시각 − 세션 시작 시각.
// verify 1회에 2~3분이 걸리므로 실제보다 그만큼 늦게 잡힌다 (두 그룹 동일).
import fs from "node:fs";
import path from "node:path";
import { KIT_DIR, RESULTS_DIR } from "../config/kit.config.mjs";
import { MILESTONES, MILESTONE_IDS, GROUPS } from "../config/milestones.mjs";

const sheetPath = process.argv[2] ?? path.join(KIT_DIR, "docs", "record-sheet.csv");
const root = process.argv[3] ?? path.join(RESULTS_DIR, "human");
const LIMIT_MIN = 40;
const MS = MILESTONE_IDS;
const MAX = MS.length;

const [header, ...rows] = fs.readFileSync(sheetPath, "utf8").trim().split("\n").map((l) => l.split(","));
const people = rows
  .filter((r) => r.length >= 3 && r[0] && !r[0].startsWith("("))
  .map((r) => Object.fromEntries(header.map((h, i) => [h.trim(), (r[i] ?? "").trim()])));

const readJsonl = (f) => (fs.existsSync(f) ? fs.readFileSync(f, "utf8").split("\n").filter(Boolean).map((l) => JSON.parse(l)) : []);

// Claude Code 트랜스크립트에서 사람이 직접 입력한 프롬프트만 센다 (다른 도구는 아직 미지원 → "-").
function countPrompts(dir, tool) {
  if (tool !== "claude") return undefined;
  const files = fs.existsSync(dir) ? fs.readdirSync(dir).filter((x) => x.endsWith(".jsonl")) : [];
  if (!files.length) return undefined;
  let n = 0;
  for (const f of files)
    for (const ev of readJsonl(path.join(dir, f))) {
      if (ev.type !== "user" || ev.isMeta || ev.isSidechain) continue;
      const c = ev.message?.content;
      const text = typeof c === "string" ? c : Array.isArray(c) && !c.some((b) => b.type === "tool_result") ? c.map((b) => b.text ?? "").join("") : "";
      if (text.trim() && !text.startsWith("<command-") && !text.startsWith("<local-command")) n++;
    }
  return n;
}

const data = people.map((p) => {
  const dir = path.join(root, p.participantId);
  const meta = fs.existsSync(path.join(dir, "meta.json")) ? JSON.parse(fs.readFileSync(path.join(dir, "meta.json"), "utf8")) : {};
  const start = new Date(p.sessionStart.replace(" ", "T")).getTime();
  const progress = readJsonl(path.join(dir, "progress.jsonl"));
  const finalJson = path.join(dir, "final.json");
  if (fs.existsSync(finalJson)) progress.push(JSON.parse(fs.readFileSync(finalJson, "utf8")));
  const reach = {};
  for (const run of progress.sort((a, b) => a.finishedAt.localeCompare(b.finishedAt))) {
    const t = (new Date(run.finishedAt).getTime() - start) / 60000;
    for (const m of MS) if (run.milestones?.[m]?.pass && reach[m] === undefined) reach[m] = Math.max(0, t);
  }
  const last = progress[progress.length - 1];
  const finalScore = last?.score ?? 0;
  const groupScores = last?.groups ?? {};
  const stat = fs.existsSync(path.join(dir, "diffstat.txt")) ? fs.readFileSync(path.join(dir, "diffstat.txt"), "utf8") : "";
  const lines = Number(stat.match(/(\d+) insertions?/)?.[1] ?? 0) + Number(stat.match(/(\d+) deletions?/)?.[1] ?? 0);
  return { ...p, aiTool: p.aiTool || meta.tool || "-", reach, finalScore, groupScores, complianceRate: last?.complianceRate, verifyRuns: progress.length, prompts: countPrompts(path.join(dir, "transcripts"), p.aiTool || meta.tool), lines };
});

const median = (xs) => {
  const a = xs.filter((x) => x != null && !Number.isNaN(x)).sort((p, q) => p - q);
  if (!a.length) return undefined;
  const m = Math.floor(a.length / 2);
  return a.length % 2 ? a[m] : (a[m - 1] + a[m]) / 2;
};
const fmt = (x, d = 1) => (x == null ? "-" : x.toFixed(d));

// Mann–Whitney U (정규 근사) + rank-biserial 효과 크기. 표본이 작으므로 p 값은 참고용이다.
function mannWhitney(a, b) {
  if (!a.length || !b.length) return undefined;
  const all = [...a.map((v) => ({ v, g: 0 })), ...b.map((v) => ({ v, g: 1 }))].sort((x, y) => x.v - y.v);
  for (let i = 0; i < all.length; ) {
    let j = i;
    while (j + 1 < all.length && all[j + 1].v === all[i].v) j++;
    for (let k = i; k <= j; k++) all[k].r = (i + j) / 2 + 1;
    i = j + 1;
  }
  const r1 = all.filter((x) => x.g === 0).reduce((s, x) => s + x.r, 0);
  const n1 = a.length, n2 = b.length;
  const u1 = r1 - (n1 * (n1 + 1)) / 2;
  const mu = (n1 * n2) / 2;
  const sigma = Math.sqrt((n1 * n2 * (n1 + n2 + 1)) / 12);
  const z = sigma ? (u1 - mu) / sigma : 0;
  const p = 2 * (1 - normCdf(Math.abs(z)));
  return { u: u1, p, effect: (2 * u1) / (n1 * n2) - 1 };
}
function normCdf(x) {
  const t = 1 / (1 + 0.2316419 * x);
  const d = 0.3989423 * Math.exp((-x * x) / 2);
  return 1 - d * t * (0.3193815 + t * (-0.3565638 + t * (1.781478 + t * (-1.821256 + t * 1.330274))));
}

const groups = { A: data.filter((d) => d.group === "A"), B: data.filter((d) => d.group === "B") };
const L = [];
L.push("# 사람 테스트 결과 (A: codegen MCP 연결 / B: 미연결)", "");
L.push(`- 참가자: A ${groups.A.length}명, B ${groups.B.length}명 · 제한 시간 ${LIMIT_MIN}분 · 만점 ${MAX}`, "");

L.push("## 1. 그룹 요약", "");
L.push(`| 그룹 | n | 만점(${MAX}/${MAX}) | 최종 점수 중앙값 | ${GROUPS.map((g) => `${g} 중앙값`).join(" | ")} | 적용률 중앙값 | 만점 도달 시간 중앙값(분) | 프롬프트 수 중앙값 | 변경 라인 중앙값 |`);
L.push(`|---|---|---|---|${GROUPS.map(() => "---").join("|")}|---|---|---|---|`);
for (const [g, ds] of Object.entries(groups)) {
  const done = ds.filter((d) => d.finalScore === MAX);
  const tMax = done.map((d) => Math.max(...MS.map((m) => d.reach[m] ?? Infinity)));
  L.push(
    `| ${g} | ${ds.length} | ${done.length}/${ds.length} | ${fmt(median(ds.map((d) => d.finalScore)))} | ${GROUPS.map((gr) => fmt(median(ds.map((d) => d.groupScores?.[gr]?.pass)))).join(" | ")} | ${fmt(median(ds.map((d) => d.complianceRate)), 0)}% | ${fmt(median(tMax))} | ${fmt(median(ds.map((d) => d.prompts)), 0)} | ${fmt(median(ds.map((d) => d.lines)), 0)} |`,
  );
}

L.push("", "## 2. 마일스톤 도달 (도달 인원 / 도달 시간 중앙값 분)", "");
L.push(`| 그룹 | ${MS.join(" | ")} |`);
L.push(`|---|${MS.map(() => "---").join("|")}|`);
for (const [g, ds] of Object.entries(groups))
  L.push(`| ${g} | ${MS.map((m) => {
    const ts = ds.map((d) => d.reach[m]).filter((x) => x != null);
    return `${ts.length}/${ds.length} · ${fmt(median(ts))}`;
  }).join(" | ")} |`);

L.push("", "## 3. 시간에 따른 평균 진척 점수 (도달 곡선)", "");
const ticks = [5, 10, 15, 20, 25, 30, 35, 40];
L.push(`| 그룹 | ${ticks.map((t) => `${t}분`).join(" | ")} |`);
L.push(`|---|${ticks.map(() => "---").join("|")}|`);
for (const [g, ds] of Object.entries(groups))
  L.push(`| ${g} | ${ticks.map((t) => {
    const s = ds.map((d) => MS.filter((m) => d.reach[m] != null && d.reach[m] <= t).length);
    return s.length ? fmt(s.reduce((a, b) => a + b, 0) / s.length) : "-";
  }).join(" | ")} |`);

L.push("", "## 4. 그룹 간 차이 (Mann–Whitney U, 참고용)", "");
const scoreTest = mannWhitney(groups.A.map((d) => d.finalScore), groups.B.map((d) => d.finalScore));
const tDone = (d) => (d.finalScore === MAX ? Math.max(...MS.map((m) => d.reach[m] ?? LIMIT_MIN)) : LIMIT_MIN + 1);
const timeTest = mannWhitney(groups.A.map(tDone), groups.B.map(tDone));
L.push("| 지표 | U | p | 효과 크기(rank-biserial, A 기준) |", "|---|---|---|---|");
if (scoreTest) L.push(`| 최종 점수 | ${fmt(scoreTest.u)} | ${fmt(scoreTest.p, 3)} | ${fmt(scoreTest.effect, 2)} |`);
if (timeTest) L.push(`| 만점 도달 시간 | ${fmt(timeTest.u)} | ${fmt(timeTest.p, 3)} | ${fmt(timeTest.effect, 2)} (음수 = A 가 빠름) |`);

L.push("", "## 5. AI 도구별 (참고 — 도구는 무작위 배정이 아니므로 인과 해석 금지)", "");
L.push("| 도구 | 그룹 | n | 최종 점수 중앙값 | 적용률 중앙값 |", "|---|---|---|---|---|");
for (const tool of [...new Set(data.map((d) => d.aiTool))].sort())
  for (const g of ["A", "B"]) {
    const ds = data.filter((d) => d.aiTool === tool && d.group === g);
    if (ds.length) L.push(`| ${tool} | ${g} | ${ds.length} | ${fmt(median(ds.map((d) => d.finalScore)))} | ${fmt(median(ds.map((d) => d.complianceRate)), 0)}% |`);
  }

L.push("", "## 6. 참가자별", "");
L.push(`| ID | 그룹 | 도구 | 모델 | eGov 경력 | AI 숙련도 | 최종 점수 | verify 실행 | 프롬프트 | ${MS.join(" | ")} |`);
L.push(`|---|---|---|---|---|---|---|---|---|${MS.map(() => "---").join("|")}|`);
for (const d of data)
  L.push(`| ${d.participantId} | ${d.group} | ${d.aiTool} | ${d.model ?? "-"} | ${d.egovYears} | ${d.aiToolSkill} | ${d.finalScore} | ${d.verifyRuns} | ${d.prompts ?? "-"} | ${MS.map((m) => fmt(d.reach[m])).join(" | ")} |`);

L.push("", "## 마일스톤 범례", "");
for (const [id, g, name] of MILESTONES) L.push(`- ${id} (${g}) ${name}`);

fs.mkdirSync(root, { recursive: true });
fs.writeFileSync(path.join(root, "report.md"), L.join("\n") + "\n");
console.log(`집계 완료: ${path.join(root, "report.md")} (${data.length}명)`);
