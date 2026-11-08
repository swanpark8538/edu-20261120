// 키트 공통 설정. 스크립트들이 import 해서 쓴다.
// 환경 변수 CODEGEN_MCP / CHECK_MCP 를 지정하면 번들 대신 그 경로를 사용한다 (개발자 PC 용).
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

export const KIT_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
export const SUBMISSIONS_DIR = path.join(KIT_DIR, "submissions");
export const MCP_BUNDLE_DIR = path.join(KIT_DIR, "mcp");
export const MCP_INSTALL_DIR = path.join(KIT_DIR, ".mcp-servers");
export const PARTICIPANT_FILE = path.join(KIT_DIR, ".participant.json");
export const TASK_FILE = path.join(KIT_DIR, "materials", "task-requirements.md");
export const REFERENCE_DIR = path.join(KIT_DIR, "reference-solution");

// 작업 영역: 저장소 밖. AI 도구는 <workspace>/<run>/ 에서 실행하고 그 안에 board-app/ 을 만든다.
export const DEFAULT_WORKSPACE_ROOT = path.join(os.homedir(), "egov-mcp-lab");
export const RUNS = ["mcp", "nomcp"]; // 실험군(MCP 연결, 20분) / 대조군(미연결, 30분)
export const RUN_MINUTES = { mcp: 20, nomcp: 30 };
export const PROJECT_NAME = "board-app";

export const BOARD_ENTITY = "Board";
export const BOARD_PACKAGE = "egovframework.example.board";

export const CODEGEN_BUNDLE = "egovframe-mcp-codegen-1.0.0.tgz";
export const CHECK_BUNDLE_PREFIX = "egovframe-mcp-check-1.0.0-";
export const MCP_SERVER_NAME = "egovframe-codegen";

export const platformKey = () => `${process.platform}-${process.arch}`;
export const codegenEntry = () =>
  process.env.CODEGEN_MCP ?? path.join(MCP_INSTALL_DIR, "egovframe-mcp-codegen", "package", "build", "index.js");
export const checkEntry = () =>
  process.env.CHECK_MCP ?? path.join(MCP_INSTALL_DIR, "egovframe-mcp-check", "package", "build", "index.js");
export const MVN = process.platform === "win32" ? "mvn.cmd" : "mvn";
// Node 18.20.2 / 20.12.2 / 21.7.2 이후는 .cmd/.bat 를 shell 없이 spawn 하면 EINVAL 을 던진다 (CVE-2024-27980 대응).
// Windows 에서 Maven 을 실행할 때는 이 옵션을 spawn 옵션에 펼쳐 넣어 셸을 거친다. 인자에 공백이 없는 경우에만 쓴다.
export const MVN_SPAWN = { shell: process.platform === "win32" };

export function readParticipant() {
  try {
    return JSON.parse(fs.readFileSync(PARTICIPANT_FILE, "utf8"));
  } catch {
    return undefined;
  }
}
export const runDir = (p, run) => path.join(p.workspace, run);
export const projectDir = (p, run) => path.join(runDir(p, run), PROJECT_NAME);
export const submissionDir = (p, run) => path.join(SUBMISSIONS_DIR, p.id, run);
