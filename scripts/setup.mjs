#!/usr/bin/env node
// 실습 환경 준비. clone 직후 1회 실행한다.
//
//   npm run setup -- --id P01 --group A --tool claude
//
//   --id     참가자 ID (진행자가 배정). 제출 브랜치 submit/<ID> 와 결과 폴더 이름이 된다.
//   --group  A = codegen MCP 를 연결, B = 연결하지 않음 (진행자가 알려 준다)
//   --tool   claude | codex | cline | none  (사용할 AI 코딩 도구. MCP 연결 설정을 도구에 맞게 만든다)
//   --skip-build   Maven 사전 빌드(의존성 내려받기) 생략
//   --no-branch    제출 브랜치를 만들지 않음
//
// 하는 일
//   1) mcp/*.tgz 를 .mcp-servers/ 에 푼다 (codegen 은 공용, check 는 이 PC 플랫폼용)
//   2) .participant.json 기록 (verify / collect 가 읽음)
//   3) 그룹 A 이면 선택한 도구용 MCP 연결 설정을 만들거나 안내한다
//   4) git 브랜치 submit/<ID> 생성
//   5) board-app 을 한 번 빌드해 ~/.m2 에 의존성을 미리 받아 둔다
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import {
  KIT_DIR, PROJECT_DIR, MCP_BUNDLE_DIR, MCP_INSTALL_DIR, PARTICIPANT_FILE, CODEGEN_BUNDLE, CHECK_BUNDLE_PREFIX,
  MCP_SERVER_NAME, MVN, platformKey, codegenEntry, checkEntry,
} from "../config/kit.config.mjs";

const argv = process.argv.slice(2);
const opt = (n, d) => {
  const i = argv.indexOf(n);
  return i >= 0 ? argv[i + 1] : d;
};
const id = opt("--id");
const group = (opt("--group") ?? "").toUpperCase();
const tool = (opt("--tool") ?? "none").toLowerCase();
const skipBuild = argv.includes("--skip-build");
const noBranch = argv.includes("--no-branch");
if (!id || !/^[\w-]+$/.test(id) || !["A", "B"].includes(group) || !["claude", "codex", "cline", "none"].includes(tool)) {
  console.error("usage: npm run setup -- --id <참가자ID> --group <A|B> --tool <claude|codex|cline|none> [--skip-build] [--no-branch]");
  process.exit(1);
}
const sh = (cmd, args, opts = {}) => spawnSync(cmd, args, { encoding: "utf8", shell: false, ...opts });
const ok = (m) => console.log(`✅ ${m}`);
const warn = (m) => console.log(`⚠️  ${m}`);
const step = (m) => console.log(`\n▶ ${m}`);

// 0) 도구 점검
step("도구 점검");
for (const [cmd, args, name] of [["java", ["-version"], "JDK 17"], [MVN, ["-version"], "Maven 3.9"], ["git", ["--version"], "Git"]]) {
  const r = sh(cmd, args);
  if (r.status === 0) ok(`${name}: ${(r.stdout || r.stderr).split("\n")[0]}`);
  else warn(`${name} 을 찾지 못했습니다 (${cmd}). PATH 를 확인하세요.`);
}
ok(`Node ${process.version} (${platformKey()})`);

// 1) MCP 번들 풀기
step("MCP 서버 번들 설치 (.mcp-servers/)");
function extract(tgz, dest) {
  if (!fs.existsSync(tgz)) return false;
  fs.rmSync(dest, { recursive: true, force: true });
  fs.mkdirSync(dest, { recursive: true });
  const r = sh("tar", ["-xzf", tgz, "-C", dest]);
  if (r.status !== 0) {
    warn(`tar 실패: ${r.stderr}`);
    return false;
  }
  return true;
}
if (extract(path.join(MCP_BUNDLE_DIR, CODEGEN_BUNDLE), path.join(MCP_INSTALL_DIR, "egovframe-mcp-codegen")) && fs.existsSync(codegenEntry()))
  ok(`egovframe-mcp-codegen → ${path.relative(KIT_DIR, codegenEntry())}`);
else warn("egovframe-mcp-codegen 번들을 설치하지 못했습니다.");
const checkTgz = path.join(MCP_BUNDLE_DIR, `${CHECK_BUNDLE_PREFIX}${platformKey()}.tgz`);
if (extract(checkTgz, path.join(MCP_INSTALL_DIR, "egovframe-mcp-check")) && fs.existsSync(checkEntry()))
  ok(`egovframe-mcp-check (${platformKey()}) → ${path.relative(KIT_DIR, checkEntry())}`);
else warn(`이 플랫폼(${platformKey()})용 egovframe-mcp-check 번들이 없습니다. 채점의 S1(표준 준수) 항목이 실패로 기록됩니다.`);

// 2) 참가자 정보
step("참가자 정보 기록");
fs.writeFileSync(PARTICIPANT_FILE, JSON.stringify({ id, group, tool, platform: platformKey(), createdAt: new Date().toISOString() }, null, 2) + "\n");
ok(`${path.relative(KIT_DIR, PARTICIPANT_FILE)} (id=${id}, group=${group}, tool=${tool})`);

// 3) MCP 연결 설정 (그룹 A 만)
step("AI 도구 MCP 연결 설정");
const codegen = codegenEntry();
const mcpJsonPath = path.join(PROJECT_DIR, ".mcp.json");
const claudeLocal = path.join(PROJECT_DIR, ".claude", "settings.local.json");
fs.rmSync(mcpJsonPath, { force: true });
if (group === "A") {
  if (!fs.existsSync(codegen)) warn("codegen 번들이 없어 MCP 연결 설정을 만들 수 없습니다.");
  else if (tool === "claude") {
    fs.writeFileSync(mcpJsonPath, JSON.stringify({ mcpServers: { [MCP_SERVER_NAME]: { type: "stdio", command: "node", args: [codegen] } } }, null, 2) + "\n");
    fs.mkdirSync(path.dirname(claudeLocal), { recursive: true });
    fs.writeFileSync(claudeLocal, JSON.stringify({ enableAllProjectMcpServers: true }, null, 2) + "\n");
    ok(`board-app/.mcp.json 작성 → board-app 에서 claude 실행 후 /mcp 로 ${MCP_SERVER_NAME} 연결 확인`);
  } else if (tool === "codex") {
    const r = sh("codex", ["mcp", "add", MCP_SERVER_NAME, "--", "node", codegen]);
    if (r.status === 0) ok(`codex mcp add ${MCP_SERVER_NAME} 완료 → codex 에서 /mcp 로 연결 확인`);
    else {
      warn("codex CLI 를 실행하지 못했습니다. ~/.codex/config.toml 에 아래를 직접 추가하세요:");
      console.log(`\n[mcp_servers.${MCP_SERVER_NAME}]\ncommand = "node"\nargs = ["${codegen.replace(/\\/g, "\\\\")}"]\n`);
    }
  } else if (tool === "cline") {
    const settings = {
      darwin: "~/Library/Application Support/Code/User/globalStorage/saoudrizwan.claude-dev/settings/cline_mcp_settings.json",
      win32: "%APPDATA%\\Code\\User\\globalStorage\\saoudrizwan.claude-dev\\settings\\cline_mcp_settings.json",
      linux: "~/.config/Code/User/globalStorage/saoudrizwan.claude-dev/settings/cline_mcp_settings.json",
    }[process.platform];
    console.log("Cline → MCP Servers → Configure MCP Servers 를 열고 (파일: " + settings + ") 아래 내용을 mcpServers 에 추가하세요:\n");
    console.log(JSON.stringify({ mcpServers: { [MCP_SERVER_NAME]: { command: "node", args: [codegen], disabled: false, autoApprove: [] } } }, null, 2), "\n");
  } else warn("--tool 을 지정하지 않아 MCP 연결 설정을 만들지 않았습니다. docs/setup-*.md 를 보고 직접 연결하세요.");
} else {
  fs.rmSync(path.join(PROJECT_DIR, ".claude"), { recursive: true, force: true });
  ok("그룹 B: 프로젝트에 MCP 설정을 두지 않습니다. 사용하는 AI 도구에 개인적으로 등록한 MCP 서버가 있다면 실습 동안 끄세요.");
}

// 4) 제출 브랜치
step("제출 브랜치");
if (noBranch) warn("--no-branch: 브랜치를 만들지 않습니다.");
else {
  const branch = `submit/${id}`;
  const cur = sh("git", ["-C", KIT_DIR, "rev-parse", "--abbrev-ref", "HEAD"]).stdout.trim();
  if (cur === branch) ok(`이미 ${branch} 브랜치입니다.`);
  else {
    const exists = sh("git", ["-C", KIT_DIR, "rev-parse", "--verify", "--quiet", `refs/heads/${branch}`]).status === 0;
    const r = sh("git", ["-C", KIT_DIR, "switch", ...(exists ? [] : ["-c"]), branch]);
    if (r.status === 0) ok(`${branch} 브랜치로 전환했습니다. 작업 결과는 이 브랜치에 커밋해서 push 합니다.`);
    else warn(`브랜치 전환 실패: ${r.stderr.trim()}`);
  }
}

// 5) Maven 사전 빌드
step("Maven 사전 빌드 (의존성 내려받기, 처음에는 몇 분 걸립니다)");
if (skipBuild) warn("--skip-build: 생략");
else {
  const r = sh(MVN, ["-q", "-B", "-DskipTests", "package"], { cwd: PROJECT_DIR, stdio: "inherit" });
  if (r.status === 0) {
    fs.rmSync(path.join(PROJECT_DIR, "target"), { recursive: true, force: true });
    ok("board-app 빌드 성공 (target/ 은 정리했습니다)");
  } else warn("board-app 빌드에 실패했습니다. 네트워크·JDK·Maven 설정을 확인하세요.");
}

console.log(`
다음 단계
  1. docs/setup-${tool === "none" ? "claude-code" : tool === "claude" ? "claude-code" : tool}.md 를 보고 AI 도구의 모델·노력 수준${group === "A" ? "과 MCP 연결" : ""}을 확인합니다.
  2. 과제는 board-app/TASK.md 입니다. 진행자가 시작을 알리면 시작합니다.
  3. 진행 중 언제든  npm run verify  로 채점합니다 (2~3분, 실행마다 results/human/${id}/progress.jsonl 에 기록).
  4. 종료 시  npm run collect  를 실행하고 안내대로 커밋·push 합니다.
`);
