#!/usr/bin/env node
// [진행자용] 정답 프로젝트(reference-solution/)를 만든다 — codegen MCP 도구 2회 호출 결과물.
// 채점기가 어디까지 통과하는지 확인(채점기 자체 검증)하는 데 쓴다. reference-solution/ 은 git 에 올라가지 않는다.
//
//   npm run reference            # 생성
//   node scripts/verify.mjs reference-solution --label reference
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { KIT_DIR, PROJECT_DIR, REFERENCE_DIR, BOARD_PACKAGE, BOARD_ENTITY, codegenEntry } from "../config/kit.config.mjs";

const codegen = codegenEntry();
if (!fs.existsSync(codegen)) {
  console.error(`codegen MCP 를 찾지 못했습니다: ${codegen}\nnpm run setup 을 먼저 실행하거나 CODEGEN_MCP 환경 변수를 지정하세요.`);
  process.exit(1);
}
fs.rmSync(REFERENCE_DIR, { recursive: true, force: true });
fs.cpSync(PROJECT_DIR, REFERENCE_DIR, {
  recursive: true,
  filter: (s) => !/[\\/](target|\.claude|\.mcp\.json|TASK\.md)$/.test(s),
});
const call = (tool, args) => {
  console.log(`▶ ${tool}`);
  const r = spawnSync(process.execPath, [path.join(KIT_DIR, "scripts", "mcp-call.mjs"), codegen, tool, JSON.stringify(args)], { encoding: "utf8" });
  if (r.status !== 0) {
    console.error(r.stdout, r.stderr);
    process.exit(r.status ?? 1);
  }
};
call("egov_add_attach_file_infrastructure", { projectPath: REFERENCE_DIR });
call("egov_add_entity", {
  projectPath: REFERENCE_DIR,
  packageName: BOARD_PACKAGE,
  entityName: BOARD_ENTITY,
  ddl: fs.readFileSync(path.join(KIT_DIR, "materials", "board.ddl.sql"), "utf8"),
  withAttachment: true,
  autoApplyMenu: true,
});
console.log(`\n✅ 정답 프로젝트 생성: ${path.relative(KIT_DIR, REFERENCE_DIR)}\n   검증: node scripts/verify.mjs reference-solution --label reference`);
