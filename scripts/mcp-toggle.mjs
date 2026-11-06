#!/usr/bin/env node
// 전역 MCP 설정을 쓰는 도구(Codex)에서 codegen MCP 를 켜고 끈다. Claude Code 는 폴더별 .mcp.json 이라 필요 없다.
//   npm run mcp -- on     mcp 실행 직전
//   npm run mcp -- off    nomcp 실행 직전
import { readParticipant, codegenEntry, MCP_SERVER_NAME } from "../config/kit.config.mjs";
import { codexToggle } from "./mcp-config.mjs";

const on = process.argv.includes("on");
const off = process.argv.includes("off");
if (on === off) {
  console.error("usage: npm run mcp -- on|off");
  process.exit(1);
}
const p = readParticipant();
if (!p) {
  console.error(".participant.json 이 없습니다. npm run setup 을 먼저 실행하세요.");
  process.exit(1);
}
if (p.tool === "codex") {
  const r = codexToggle(on, codegenEntry());
  console.log(r.out.trim());
  console.log(r.ok ? `✅ codex: ${MCP_SERVER_NAME} ${on ? "추가" : "제거"} 완료. codex 를 다시 시작하고 /mcp 로 확인하세요.` : "⚠️  codex 명령 실패. ~/.codex/config.toml 을 직접 편집하세요.");
} else if (p.tool === "cline") {
  console.log(`Cline 은 MCP Servers 화면에서 ${MCP_SERVER_NAME} 을 ${on ? "켜(disabled: false)" : "끄"}세요. 파일을 직접 고치려면 cline_mcp_settings.json 의 "disabled" 값을 바꿉니다.`);
} else if (p.tool === "cursor") {
  console.log(`Cursor 는 mcp 폴더의 .cursor/mcp.json 으로만 연결되므로 켜고 끌 필요가 없습니다. ${on ? "mcp" : "nomcp"} 폴더를 Cursor 로 여세요 (전역 ~/.cursor/mcp.json 의 개인 서버는 Settings → MCP 에서 끕니다).`);
} else {
  console.log(`Claude Code 는 mcp 폴더의 .mcp.json 으로만 연결되므로 켜고 끌 필요가 없습니다. ${on ? "mcp" : "nomcp"} 폴더에서 claude 를 실행하세요.`);
}
