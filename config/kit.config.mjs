// 키트 공통 설정. 스크립트들이 import 해서 쓴다.
// 환경 변수 CODEGEN_MCP / CHECK_MCP 를 지정하면 번들 대신 그 경로를 사용한다 (개발자 PC 용).
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

export const KIT_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
export const PROJECT_DIR = path.join(KIT_DIR, "board-app");
export const REFERENCE_DIR = path.join(KIT_DIR, "reference-solution");
export const RESULTS_DIR = path.join(KIT_DIR, "results");
export const MCP_BUNDLE_DIR = path.join(KIT_DIR, "mcp");
export const MCP_INSTALL_DIR = path.join(KIT_DIR, ".mcp-servers");
export const PARTICIPANT_FILE = path.join(KIT_DIR, ".participant.json");

// 기반 프로젝트 식별자 (board-app 과 일치해야 한다)
export const PROJECT_NAME = "board-app";
export const BASE_PACKAGE = "egovframework.example";
export const SAMPLE_ENTITY = "Dept";
export const SAMPLE_PACKAGE = "egovframework.example.dept";
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

export function readParticipant() {
  try {
    return JSON.parse(fs.readFileSync(PARTICIPANT_FILE, "utf8"));
  } catch {
    return undefined;
  }
}
