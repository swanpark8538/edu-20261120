#!/usr/bin/env node
// [진행자용] codegen 도구만으로 만들 수 있는 참조 프로젝트를 reference-solution/ 에 만든다 (git 제외).
// 채점기가 어디까지 통과하는지(채점기 자체 검증) 확인하는 데 쓴다.
//
//   npm run reference            egov_scaffold_project(Board) → egov_add_attach_file_infrastructure
//   node scripts/verify.mjs --project reference-solution/board-app --label reference
//
// 현재 codegen 은 빈 폴더에서 "첨부가 연결된 Board" 를 도구 호출만으로 만들지 못한다 (scaffold 에 withAttachment 없음,
// 같은 엔티티의 add_entity 는 충돌로 거부). 따라서 이 참조 프로젝트는 F5·F6·S1(첨부) 항목이 실패하는 것이 정상이다.
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { KIT_DIR, REFERENCE_DIR, BOARD_PACKAGE, BOARD_ENTITY, PROJECT_NAME, codegenEntry } from "../config/kit.config.mjs";

const codegen = codegenEntry();
if (!fs.existsSync(codegen)) {
  console.error(`codegen MCP 를 찾지 못했습니다: ${codegen}\nnpm run setup 을 먼저 실행하거나 CODEGEN_MCP 환경 변수를 지정하세요.`);
  process.exit(1);
}
fs.rmSync(REFERENCE_DIR, { recursive: true, force: true });
fs.mkdirSync(REFERENCE_DIR, { recursive: true });
const call = (tool, args) => {
  console.log(`▶ ${tool}`);
  const r = spawnSync(process.execPath, [path.join(KIT_DIR, "scripts", "mcp-call.mjs"), codegen, tool, JSON.stringify(args)], { encoding: "utf8" });
  if (r.status !== 0) {
    console.error(r.stdout, r.stderr);
    process.exit(r.status ?? 1);
  }
};
call("egov_scaffold_project", {
  projectName: PROJECT_NAME,
  packageName: BOARD_PACKAGE,
  outputPath: REFERENCE_DIR,
  entityName: BOARD_ENTITY,
  ddl: fs.readFileSync(path.join(KIT_DIR, "materials", "board.ddl.sql"), "utf8"),
  description: "참조 프로젝트",
});
call("egov_add_attach_file_infrastructure", { projectPath: path.join(REFERENCE_DIR, PROJECT_NAME) });
console.log(`\n✅ 참조 프로젝트 생성: ${path.relative(KIT_DIR, path.join(REFERENCE_DIR, PROJECT_NAME))}\n   검증: node scripts/verify.mjs --project reference-solution/${PROJECT_NAME} --label reference`);
