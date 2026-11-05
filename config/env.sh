# bash 사용자를 위한 환경 변수. 스크립트는 모두 Node 로 동작하므로 필수는 아니다.
KIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${CODEGEN_MCP:=$KIT_DIR/.mcp-servers/egovframe-mcp-codegen/package/build/index.js}"
: "${CHECK_MCP:=$KIT_DIR/.mcp-servers/egovframe-mcp-check/package/build/index.js}"
export KIT_DIR CODEGEN_MCP CHECK_MCP
