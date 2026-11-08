#!/usr/bin/env node
// 실습 환경 준비. clone 직후 1회 실행한다.
//
//   npm run setup -- --id P01 --tool claude --order mcp-first
//
//   --id        참가자 ID (진행자가 배정). 제출 브랜치 submit/<ID> 와 submissions/<ID>/ 이름이 된다.
//   --tool      claude | codex | cursor | cline   사용할 AI 코딩 도구 (MCP 연결 방식이 도구마다 다르다)
//   --order     mcp-first | nomcp-first  두 실행의 순서 (진행자가 알려 준다. 기본: ID 끝자리가 홀수면 mcp-first)
//   --workspace 작업 영역 루트 (기본 ~/egov-mcp-lab/<ID>). 저장소 밖이어야 한다.
//   --skip-warmup   Maven 의존성 미리 받기 생략
//   --no-branch     제출 브랜치를 만들지 않음
//
// 하는 일
//   1) mcp/*.tgz 를 .mcp-servers/ 에 푼다
//   2) 작업 영역 <workspace>/mcp/ 와 <workspace>/nomcp/ 를 만들고 각각 TASK.md 를 둔다
//      (mcp/ 에만 AI 도구용 MCP 연결 설정). AI 도구는 이 폴더에서 실행하고, 그 안에 board-app/ 을 만든다.
//   3) .participant.json 기록 (verify / collect 가 읽음)
//   4) git 브랜치 submit/<ID> 생성
//   5) codegen 으로 임시 프로젝트를 만들어 한 번 빌드해 ~/.m2 에 의존성을 미리 받아 둔다 (임시 프로젝트는 삭제)
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import {
  KIT_DIR, MCP_BUNDLE_DIR, MCP_INSTALL_DIR, PARTICIPANT_FILE, TASK_FILE, CODEGEN_BUNDLE, CHECK_BUNDLE_PREFIX,
  MCP_SERVER_NAME, MVN, MVN_SPAWN, DEFAULT_WORKSPACE_ROOT, RUNS, RUN_MINUTES, PROJECT_NAME, platformKey, codegenEntry, checkEntry,
} from "../config/kit.config.mjs";
import { mcpConfigForTool } from "./mcp-config.mjs";

const argv = process.argv.slice(2);
const opt = (n, d) => {
  const i = argv.indexOf(n);
  return i >= 0 ? argv[i + 1] : d;
};
const id = opt("--id");
const tool = (opt("--tool") ?? "").toLowerCase();
const defaultOrder = /[13579]$/.test(id ?? "") ? "mcp-first" : "nomcp-first";
const order = opt("--order", defaultOrder);
const workspace = path.resolve(opt("--workspace", path.join(DEFAULT_WORKSPACE_ROOT, id ?? "")));
const skipWarmup = argv.includes("--skip-warmup");
const noBranch = argv.includes("--no-branch");
if (!id || !/^[\w-]+$/.test(id) || !["claude", "codex", "cursor", "cline"].includes(tool) || !["mcp-first", "nomcp-first"].includes(order)) {
  console.error("usage: npm run setup -- --id <참가자ID> --tool <claude|codex|cursor|cline> [--order mcp-first|nomcp-first] [--workspace <dir>] [--skip-warmup] [--no-branch]");
  process.exit(1);
}
if (workspace.startsWith(KIT_DIR + path.sep) || workspace === KIT_DIR) {
  console.error(`작업 영역은 저장소(${KIT_DIR}) 밖에 두어야 합니다. AI 도구가 채점 키트를 읽지 못하게 하기 위해서입니다.`);
  process.exit(1);
}
const sh = (cmd, args, opts = {}) => spawnSync(cmd, args, { encoding: "utf8", shell: false, ...opts });
const ok = (m) => console.log(`✅ ${m}`);
const warn = (m) => console.log(`⚠️  ${m}`);
const step = (m) => console.log(`\n▶ ${m}`);

// 0) 도구 점검
step("도구 점검");
for (const [cmd, args, name, opts] of [["java", ["-version"], "JDK 17"], [MVN, ["-version"], "Maven 3.9", MVN_SPAWN], ["git", ["--version"], "Git"]]) {
  const r = sh(cmd, args, opts);
  if (r.status === 0) ok(`${name}: ${(r.stdout || r.stderr).split("\n")[0]}`);
  else warn(`${name} 을 찾지 못했습니다 (${cmd}). PATH 를 확인하세요.`);
}
ok(`Node ${process.version} (${platformKey()})`);

// 1) MCP 번들
step("MCP 서버 번들 설치 (.mcp-servers/)");
function extract(tgz, dest) {
  if (!fs.existsSync(tgz)) return false;
  fs.rmSync(dest, { recursive: true, force: true });
  fs.mkdirSync(dest, { recursive: true });
  const r = sh("tar", ["-xzf", tgz, "-C", dest]);
  if (r.status !== 0) warn(`tar 실패: ${r.stderr}`);
  return r.status === 0;
}
if (extract(path.join(MCP_BUNDLE_DIR, CODEGEN_BUNDLE), path.join(MCP_INSTALL_DIR, "egovframe-mcp-codegen")) && fs.existsSync(codegenEntry()))
  ok(`egovframe-mcp-codegen → ${path.relative(KIT_DIR, codegenEntry())}`);
else warn("egovframe-mcp-codegen 번들을 설치하지 못했습니다.");
if (extract(path.join(MCP_BUNDLE_DIR, `${CHECK_BUNDLE_PREFIX}${platformKey()}.tgz`), path.join(MCP_INSTALL_DIR, "egovframe-mcp-check")) && fs.existsSync(checkEntry()))
  ok(`egovframe-mcp-check (${platformKey()}) → ${path.relative(KIT_DIR, checkEntry())}`);
else warn(`이 플랫폼(${platformKey()})용 egovframe-mcp-check 번들이 없습니다. R1(표준 준수율) 항목이 실패로 기록됩니다.`);

// 2) 작업 영역
step(`작업 영역 준비: ${workspace}`);
const task = fs.readFileSync(TASK_FILE, "utf8");
for (const run of RUNS) {
  const dir = path.join(workspace, run);
  fs.mkdirSync(dir, { recursive: true });
  const header = run === "mcp"
    ? `> **실험군 실행 (${RUN_MINUTES.mcp}분)** — 이 폴더의 AI 도구에는 eGovFrame 코드 생성 MCP(\`${MCP_SERVER_NAME}\`)가 연결되어 있습니다.\n\n`
    : `> **대조군 실행 (${RUN_MINUTES.nomcp}분)** — 이 폴더에서는 MCP 없이 AI 도구의 기본 기능만 사용합니다.\n\n`;
  fs.writeFileSync(path.join(dir, "TASK.md"), header + task);
  ok(`${run}/TASK.md${fs.existsSync(path.join(dir, PROJECT_NAME)) ? " (board-app 이 이미 있음 — 그대로 둠)" : ""}`);
}
// 작업 영역이 다른 git 저장소 안이면 AI 도구가 상위를 뒤질 수 있으므로 경고만 남긴다.
if (sh("git", ["-C", workspace, "rev-parse", "--show-toplevel"]).status === 0) warn("작업 영역이 git 저장소 안에 있습니다. 가능하면 저장소 밖 경로를 --workspace 로 지정하세요.");

// 3) MCP 연결 설정 (mcp 실행 폴더에만)
step("AI 도구 MCP 연결 설정");
const cfg = mcpConfigForTool(tool, path.join(workspace, "mcp"), codegenEntry());
fs.rmSync(path.join(workspace, "nomcp", ".mcp.json"), { force: true });
fs.rmSync(path.join(workspace, "nomcp", ".claude"), { recursive: true, force: true });
fs.rmSync(path.join(workspace, "nomcp", ".cursor"), { recursive: true, force: true });
for (const [file, content] of Object.entries(cfg.files)) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, content);
  ok(`${path.relative(workspace, file)} 작성`);
}
for (const line of cfg.notes) console.log(line);

// 4) 참가자 정보
step("참가자 정보 기록");
fs.writeFileSync(PARTICIPANT_FILE, JSON.stringify({ id, tool, order, workspace, platform: platformKey(), createdAt: new Date().toISOString() }, null, 2) + "\n");
ok(`${path.relative(KIT_DIR, PARTICIPANT_FILE)} (id=${id}, tool=${tool}, order=${order})`);

// 5) 제출 브랜치
step("제출 브랜치");
if (noBranch) warn("--no-branch: 브랜치를 만들지 않습니다.");
else {
  const branch = `submit/${id}`;
  const cur = sh("git", ["-C", KIT_DIR, "rev-parse", "--abbrev-ref", "HEAD"]).stdout.trim();
  if (cur === branch) ok(`이미 ${branch} 브랜치입니다.`);
  else {
    const exists = sh("git", ["-C", KIT_DIR, "rev-parse", "--verify", "--quiet", `refs/heads/${branch}`]).status === 0;
    const r = sh("git", ["-C", KIT_DIR, "switch", ...(exists ? [] : ["-c"]), branch]);
    if (r.status === 0) ok(`${branch} 브랜치로 전환했습니다. 제출물은 이 브랜치에 커밋해서 push 합니다.`);
    else warn(`브랜치 전환 실패: ${r.stderr.trim()}`);
  }
}

// 6) Maven 의존성 미리 받기 — codegen 으로 임시 프로젝트를 만들어 한 번 빌드한다 (수강생 작업 영역과 무관, 끝나면 삭제)
step("Maven 의존성 미리 받기 (처음에는 몇 분 걸립니다)");
if (skipWarmup) warn("--skip-warmup: 생략");
else if (!fs.existsSync(codegenEntry())) warn("codegen 번들이 없어 생략합니다.");
else {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), "egov-warmup-"));
  const args = JSON.stringify({ projectName: "warmup-app", packageName: "egovframework.warmup", outputPath: tmp, entityName: "Warmup", ddl: "CREATE TABLE WARMUP (WARMUP_ID VARCHAR(20) NOT NULL, NM VARCHAR(50), PRIMARY KEY (WARMUP_ID));", description: "dependency warm-up" });
  const r1 = sh(process.execPath, [path.join(KIT_DIR, "scripts", "mcp-call.mjs"), codegenEntry(), "egov_scaffold_project", args]);
  if (r1.status !== 0) warn(`임시 프로젝트 생성 실패: ${r1.stderr || r1.stdout}`);
  else {
    const r2 = sh(process.execPath, [path.join(KIT_DIR, "scripts", "mcp-call.mjs"), codegenEntry(), "egov_add_attach_file_infrastructure", JSON.stringify({ projectPath: path.join(tmp, "warmup-app") })]);
    if (r2.status !== 0) warn("첨부파일 인프라 의존성은 받지 못했습니다 (계속 진행).");
    const r3 = sh(MVN, ["-q", "-B", "-DskipTests", "package"], { cwd: path.join(tmp, "warmup-app"), stdio: "inherit", ...MVN_SPAWN });
    if (r3.status === 0) ok("의존성 다운로드 완료 (~/.m2)");
    else warn("임시 프로젝트 빌드 실패. 네트워크·JDK·Maven 설정을 확인하세요.");
  }
  fs.rmSync(tmp, { recursive: true, force: true });
}

const first = order === "mcp-first" ? "mcp" : "nomcp";
const second = first === "mcp" ? "nomcp" : "mcp";
console.log(`
다음 단계
  1. docs/setup-${tool === "claude" ? "claude-code" : tool}.md 를 보고 모델·노력 수준과 MCP 연결(mcp 폴더)·미연결(nomcp 폴더)을 확인합니다.
  2. 실행 순서: ① ${first} (${RUN_MINUTES[first]}분) → ② ${second} (${RUN_MINUTES[second]}분). AI 도구는 반드시 아래 폴더에서 실행합니다.
       ${path.join(workspace, first)}
       ${path.join(workspace, second)}
     이 저장소(${KIT_DIR}) 는 채점 키트이므로 AI 도구의 작업 영역으로 열지 마세요.
  3. 각 실행 시작 직전과 종료 직후에  npm run verify -- ${first}  /  npm run verify -- ${second}  를 실행합니다 (중간에도 1회 권장).
  4. 모두 끝나면  npm run collect  를 실행하고 안내대로 커밋·push 합니다.
`);
