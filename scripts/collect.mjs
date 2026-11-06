#!/usr/bin/env node
// 두 실행이 모두 끝난 뒤 실행. 실행별 최종 채점 + 산출물 사본 + 규모 + AI 도구 대화 기록을 submissions/<ID>/<run>/ 에 모은다.
//
//   npm run collect            두 실행 모두
//   npm run collect -- mcp     한 실행만
//
// submissions/<ID>/<run>/
//   final.json        최종 채점 결과          progress.jsonl  verify 실행 기록 (이미 쌓여 있던 것)
//   project/          산출물 사본 (target/.git 제외, 정적 이미지·폰트 제외)   excluded-files.txt  제외한 파일 목록
//   size.json         파일 수·소스 라인 수    transcripts/    AI 도구 대화 기록 사본 (git 제외)
// submissions/<ID>/meta.json  참가자 ID·도구·순서·플랫폼
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { KIT_DIR, RUNS, SUBMISSIONS_DIR, readParticipant, projectDir, runDir, submissionDir } from "../config/kit.config.mjs";

const p = readParticipant();
if (!p) {
  console.error(".participant.json 이 없습니다. npm run setup 을 먼저 실행하세요.");
  process.exit(1);
}
const only = process.argv.slice(2).find((a) => RUNS.includes(a));
const runs = only ? [only] : RUNS;
const sh = (cmd, args, opts = {}) => spawnSync(cmd, args, { encoding: "utf8", ...opts });
const SRC_EXT = /\.(java|xml|properties|html|js|css|sql|yml|yaml|json|md)$/i;
const HEAVY = /[\\/]src[\\/]main[\\/]resources[\\/]static[\\/](img|images|fonts)[\\/]/;

for (const run of runs) {
  const proj = projectDir(p, run);
  const out = submissionDir(p, run);
  fs.mkdirSync(out, { recursive: true });
  console.log(`\n══════ ${run} (${proj}) ══════`);

  console.log(`▶ 최종 채점`);
  sh(process.execPath, [path.join(KIT_DIR, "scripts", "verify.mjs"), run, "--label", `${p.id}-${run}-final`, "--json", path.join(out, "final.json"), "--log", path.join(out, "progress.jsonl")], { stdio: "inherit" });

  console.log(`\n▶ 산출물 사본`);
  const dest = path.join(out, "project");
  fs.rmSync(dest, { recursive: true, force: true });
  const excluded = [];
  let files = 0;
  let lines = 0;
  if (fs.existsSync(path.join(proj, "pom.xml"))) {
    fs.cpSync(proj, dest, {
      recursive: true,
      filter: (src) => {
        if (/[\\/](target|\.git|node_modules|\.claude|\.cursor|\.mcp\.json)$/.test(src)) return false;
        if (HEAVY.test(src + path.sep) && fs.statSync(src).isFile()) {
          excluded.push(`${path.relative(proj, src)}\t${fs.statSync(src).size}`);
          return false;
        }
        return true;
      },
    });
    for (const f of walk(dest)) {
      files++;
      if (SRC_EXT.test(f)) lines += fs.readFileSync(f, "utf8").split("\n").length;
    }
    fs.writeFileSync(path.join(out, "excluded-files.txt"), excluded.join("\n") + (excluded.length ? "\n" : ""));
    console.log(`  ${files}개 파일, 소스 ${lines}줄 복사 (정적 이미지·폰트 ${excluded.length}개 제외)`);
  } else console.log("  board-app 이 없어 복사하지 않습니다.");
  fs.writeFileSync(path.join(out, "size.json"), JSON.stringify({ files, sourceLines: lines, excludedStatic: excluded.length }, null, 2) + "\n");

  console.log(`▶ AI 도구 대화 기록 사본 (로컬 보관용, git 제외)`);
  const tdir = path.join(out, "transcripts");
  fs.mkdirSync(tdir, { recursive: true });
  console.log(`  ${copyTranscripts(p, runDir(p, run), tdir)}개 파일 → ${path.relative(KIT_DIR, tdir)}`);
}
fs.writeFileSync(path.join(SUBMISSIONS_DIR, p.id, "meta.json"), JSON.stringify({ ...p, collectedAt: new Date().toISOString() }, null, 2) + "\n");

const branch = sh("git", ["-C", KIT_DIR, "rev-parse", "--abbrev-ref", "HEAD"]).stdout.trim();
console.log(`
✅ 수집 완료: ${path.relative(KIT_DIR, path.join(SUBMISSIONS_DIR, p.id))}

이제 결과를 제출합니다 (현재 브랜치: ${branch}).
`);
console.log(`git add submissions/${p.id}`);
console.log(`git commit -m "submit: ${p.id} (${p.tool}, ${p.order})"`);
console.log(`git push -u origin ${branch}`);
console.log(`
push 권한이 없으면 저장소를 fork 한 뒤 fork 로 push 하고 Pull Request 를 올리세요.`);

function walk(dir, acc = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const f = path.join(dir, e.name);
    if (e.isDirectory()) walk(f, acc);
    else acc.push(f);
  }
  return acc;
}
// 실습 시작(setup) 이후 생성·수정된 기록만 복사한다. 작업 폴더 경로가 파일명이나 내용에 들어가는 도구는 그것으로 거른다.
function copyTranscripts(p, workDir, tdir) {
  const since = new Date(p.createdAt).getTime();
  let n = 0;
  const copyNewer = (dir, pred) => {
    if (!fs.existsSync(dir)) return;
    for (const f of walk(dir)) {
      if (!pred(f) || fs.statSync(f).mtimeMs < since) continue;
      fs.copyFileSync(f, path.join(tdir, `${path.basename(path.dirname(f))}__${path.basename(f)}`));
      n++;
    }
  };
  if (p.tool === "claude") {
    // ~/.claude/projects/<작업 폴더 절대 경로의 영숫자 외 문자를 - 로 바꾼 이름>/*.jsonl  (하위 board-app 에서 실행한 경우도 포함)
    const root = path.join(os.homedir(), ".claude", "projects");
    const key = workDir.replace(/[^a-zA-Z0-9]/g, "-");
    if (fs.existsSync(root)) for (const d of fs.readdirSync(root)) if (d.startsWith(key)) copyNewer(path.join(root, d), (f) => f.endsWith(".jsonl"));
  } else if (p.tool === "codex") copyNewer(path.join(os.homedir(), ".codex", "sessions"), (f) => f.endsWith(".jsonl") && fs.readFileSync(f, "utf8").includes(workDir));
  else if (p.tool === "cursor") {
    // Cursor 의 대화 기록은 SQLite(state.vscdb) 안에 있어 파일 단위로 복사하지 않는다. 필요하면 진행자가 Cursor 의 내보내기 기능으로 받는다.
  } else if (p.tool === "cline") {
    const base = { darwin: path.join(os.homedir(), "Library/Application Support/Code/User/globalStorage/saoudrizwan.claude-dev/tasks"), win32: path.join(process.env.APPDATA ?? "", "Code/User/globalStorage/saoudrizwan.claude-dev/tasks"), linux: path.join(os.homedir(), ".config/Code/User/globalStorage/saoudrizwan.claude-dev/tasks") }[process.platform];
    copyNewer(base, (f) => f.endsWith(".json"));
  }
  return n;
}
