# MCP 서버 번들

`npm run setup` 이 이 디렉터리의 tgz 를 `.mcp-servers/` 에 풀어 사용합니다. 직접 풀려면 `tar -xzf <파일> -C <대상>` → `<대상>/package/build/index.js` 가 실행 진입점입니다.

| 파일 | 용도 | 비고 |
|---|---|---|
| `egovframe-mcp-codegen-1.0.0.tgz` | eGovFrame 코드 생성 MCP (그룹 A 가 AI 도구에 연결) | 순수 JS, 모든 플랫폼 공용. 2026-10-30 소스(`fs-extra` import 수정 포함)로 빌드 |
| `egovframe-mcp-check-1.0.0-<platform>.tgz` | eGovFrame 표준 준수 검사 MCP (채점기 S1 이 직접 호출) | ripgrep 바이너리가 플랫폼별이라 darwin-arm64 / darwin-x64 / linux-x64 / linux-arm64 / win32-x64 로 나뉨 |

소스: `egovframe-mcp-codegen`, `egovframe-mcp-check` 저장소의 `npm run pack:bundle` / `npm run pack:platforms` 산출물입니다.
check MCP 는 앞으로 크게 바뀔 예정이므로, 새 번들을 받으면 같은 이름으로 교체하고 `npm run setup` 을 다시 실행합니다.
