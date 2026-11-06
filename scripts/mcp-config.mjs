// AI 도구별 codegen MCP 연결 설정. setup 과 mcp-toggle 이 공유한다.
//
//   claude : 프로젝트 폴더의 .mcp.json 으로 연결 → mcp 폴더에만 두면 nomcp 폴더에서는 자동으로 미연결
//   codex  : ~/.codex/config.toml 전역 설정 → nomcp 실행 전에 제거하고 mcp 실행 전에 추가해야 한다 (npm run mcp -- on|off)
//   cline  : VS Code 전역 cline_mcp_settings.json → MCP Servers 화면에서 켜고 꺼야 한다
import { spawnSync } from "node:child_process";
import path from "node:path";
import { MCP_SERVER_NAME } from "../config/kit.config.mjs";

export function mcpConfigForTool(tool, mcpRunDir, codegenEntry) {
  const files = {};
  const notes = [];
  if (tool === "claude") {
    files[path.join(mcpRunDir, ".mcp.json")] = JSON.stringify({ mcpServers: { [MCP_SERVER_NAME]: { type: "stdio", command: "node", args: [codegenEntry] } } }, null, 2) + "\n";
    files[path.join(mcpRunDir, ".claude", "settings.local.json")] = JSON.stringify({ enableAllProjectMcpServers: true }, null, 2) + "\n";
    notes.push(`   mcp 폴더에서 claude 실행 → /mcp 에 ${MCP_SERVER_NAME} connected 확인. nomcp 폴더에서는 /mcp 가 비어 있어야 합니다.`);
  } else if (tool === "codex") {
    notes.push(`   Codex 의 MCP 설정은 전역(~/.codex/config.toml)입니다. 실행 전마다 아래 명령으로 켜고 끕니다:`);
    notes.push(`     mcp 실행 직전:   npm run mcp -- on      (codex mcp add ${MCP_SERVER_NAME} -- node ${codegenEntry})`);
    notes.push(`     nomcp 실행 직전: npm run mcp -- off     (codex mcp remove ${MCP_SERVER_NAME})`);
  } else if (tool === "cline") {
    const settings = {
      darwin: "~/Library/Application Support/Code/User/globalStorage/saoudrizwan.claude-dev/settings/cline_mcp_settings.json",
      win32: "%APPDATA%\\Code\\User\\globalStorage\\saoudrizwan.claude-dev\\settings\\cline_mcp_settings.json",
      linux: "~/.config/Code/User/globalStorage/saoudrizwan.claude-dev/settings/cline_mcp_settings.json",
    }[process.platform];
    notes.push(`   Cline → MCP Servers → Configure MCP Servers (${settings}) 의 mcpServers 에 아래를 추가합니다:`);
    notes.push(JSON.stringify({ mcpServers: { [MCP_SERVER_NAME]: { command: "node", args: [codegenEntry], disabled: false, autoApprove: [] } } }, null, 2));
    notes.push(`   nomcp 실행 전에는 MCP Servers 화면에서 ${MCP_SERVER_NAME} 을 끄고(disabled), mcp 실행 전에 다시 켭니다.`);
  }
  return { files, notes };
}

// codex 전역 MCP 켜기/끄기. 성공 여부를 돌려준다.
export function codexToggle(on, codegenEntry) {
  const r = on
    ? spawnSync("codex", ["mcp", "add", MCP_SERVER_NAME, "--", "node", codegenEntry], { encoding: "utf8" })
    : spawnSync("codex", ["mcp", "remove", MCP_SERVER_NAME], { encoding: "utf8" });
  return { ok: r.status === 0, out: (r.stdout || "") + (r.stderr || "") };
}
