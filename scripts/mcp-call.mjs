#!/usr/bin/env node
// stdio MCP 서버의 도구 하나를 호출하고 텍스트 결과를 출력한다.
// 사용법: node scripts/mcp-call.mjs <server build/index.js> <tool name> <args.json | inline JSON>
// 도구가 isError 를 반환하면 exit 2, 연결 실패 등은 exit 1.
import { readFileSync, existsSync } from "node:fs";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const [serverPath, toolName, argsSrc = "{}"] = process.argv.slice(2);
if (!serverPath || !toolName) {
  console.error("usage: mcp-call.mjs <server build/index.js> <tool> <args.json|json>");
  process.exit(1);
}
const args = JSON.parse(existsSync(argsSrc) ? readFileSync(argsSrc, "utf8") : argsSrc);

const transport = new StdioClientTransport({ command: process.execPath, args: [serverPath], stderr: "ignore" });
const client = new Client({ name: "edu-kit", version: "1.0.0" });

try {
  await client.connect(transport);
  const result = await client.callTool({ name: toolName, arguments: args }, undefined, { timeout: 300_000 });
  for (const c of result.content ?? []) if (c.type === "text") console.log(c.text);
  await client.close();
  process.exit(result.isError ? 2 : 0);
} catch (e) {
  console.error(`[mcp-call] ${toolName} 호출 실패: ${e instanceof Error ? e.message : e}`);
  await client.close().catch(() => {});
  process.exit(1);
}
