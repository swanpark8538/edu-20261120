#!/usr/bin/env node
// 종료 직후 실행. 최종 채점 + 변경 규모 + AI 도구 대화 기록을 results/human/<ID>/ 에 모은다.
//
//   npm run collect
//
// 결과 (results/human/<ID>/)
//   final.json       최종 채점 결과
//   progress.jsonl   verify 실행 기록 (이미 쌓여 있던 것)
//   diffstat.txt     main 대비 board-app 변경 규모
//   meta.json        참가자 ID·그룹·도구·플랫폼
//   transcripts/     AI 도구 대화 기록 사본 (git 에는 올라가지 않음 — 진행자가 요청할 때만 전달)
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { KIT_DIR, PROJECT_DIR, RESULTS_DIR, readParticipant } from "../config/kit.config.mjs";

const p = readParticipant();
if (!p) {
  console.error(".participant.json 이 없습니다. npm run setup 을 먼저 실행하세요.");
  process.exit(1);
}
const out = path.join(RESULTS_DIR, "human", p.id);
fs.mkdirSync(out, { recursive: true });
const sh = (cmd, args, opts = {}) => spawnSync(cmd, args, { encoding: "utf8", ...opts });

console.log(`▶ 최종 채점 (${p.id})`);
sh(process.execPath, [path.join(KIT_DIR, "scripts", "verify.mjs"), PROJECT_DIR, "--label", `${p.id}-final`, "--json", path.join(out, "final.json"), "--log", path.join(out, "progress.jsonl")], { stdio: "inherit" });

console.log("\n▶ 변경 규모 (main 대비 board-app)");
sh("git", ["-C", KIT_DIR, "add", "-A", "board-app"]);
const stat = sh("git", ["-C", KIT_DIR, "diff", "--cached", "--shortstat", "main", "--", "board-app"]).stdout.trim();
fs.writeFileSync(path.join(out, "diffstat.txt"), stat + "\n");
console.log("  " + (stat || "(변경 없음)"));

fs.writeFileSync(path.join(out, "meta.json"), JSON.stringify({ ...p, collectedAt: new Date().toISOString() }, null, 2) + "\n");

console.log("\n▶ AI 도구 대화 기록 사본 (로컬 보관용, git 제외)");
const tdir = path.join(out, "transcripts");
fs.mkdirSync(tdir, { recursive: true });
const since = new Date(p.createdAt).getTime();
const copyNewer = (dir, pred = () => true) => {
  if (!fs.existsSync(dir)) return 0;
  let n = 0;
  for (const f of fs.readdirSync(dir, { withFileTypes: true, recursive: true })) {
    if (!f.isFile() || !pred(f.name)) continue;
    const full = path.join(f.parentPath ?? f.path, f.name);
    if (fs.statSync(full).mtimeMs < since) continue;
    fs.copyFileSync(full, path.join(tdir, `${path.basename(path.dirname(full))}__${f.name}`));
    n++;
  }
  return n;
};
let copied = 0;
if (p.tool === "claude") {
  // ~/.claude/projects/<프로젝트 절대 경로의 영숫자 외 문자를 - 로 바꾼 이름>/*.jsonl
  const dir = path.join(os.homedir(), ".claude", "projects", PROJECT_DIR.replace(/[^a-zA-Z0-9]/g, "-"));
  copied = copyNewer(dir, (n) => n.endsWith(".jsonl"));
} else if (p.tool === "codex") copied = copyNewer(path.join(os.homedir(), ".codex", "sessions"), (n) => n.endsWith(".jsonl"));
else if (p.tool === "cline") {
  const base = { darwin: path.join(os.homedir(), "Library/Application Support/Code/User/globalStorage/saoudrizwan.claude-dev/tasks"), win32: path.join(process.env.APPDATA ?? "", "Code/User/globalStorage/saoudrizwan.claude-dev/tasks"), linux: path.join(os.homedir(), ".config/Code/User/globalStorage/saoudrizwan.claude-dev/tasks") }[process.platform];
  copied = copyNewer(base, (n) => n.endsWith(".json"));
}
console.log(`  ${copied}개 파일 복사 → ${path.relative(KIT_DIR, tdir)}`);

const branch = sh("git", ["-C", KIT_DIR, "rev-parse", "--abbrev-ref", "HEAD"]).stdout.trim();
console.log(`
✅ 수집 완료: ${path.relative(KIT_DIR, out)}

이제 결과를 제출합니다 (현재 브랜치: ${branch}).
`);
console.log(`git add -A`);
console.log(`git commit -m "submit: ${p.id} (group ${p.group}, ${p.tool})"`);
console.log(`git push -u origin ${branch}`);
console.log(`
push 권한이 없으면 저장소를 fork 한 뒤 fork 로 push 하고 Pull Request 를 올리세요.`);
