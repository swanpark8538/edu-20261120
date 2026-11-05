# Codex 설정

## 모델 · 노력 수준

실습에서는 **GPT-6.1 Sol, 추론 노력(reasoning effort) medium** 으로 맞춥니다.

1. `board-app/` 에서 `codex` 를 실행합니다.
2. `/model` 로 **GPT-6.1 Sol** 을 선택하고 추론 노력을 **medium** 으로 둡니다.
3. 설정 파일로 고정하려면 `~/.codex/config.toml` 에 다음을 둡니다 (모델 ID 는 `/model` 화면에 표시되는 값을 그대로 쓰세요). 실습이 끝난 뒤 원래 값으로 되돌립니다.

```toml
model = "<GPT-6.1 Sol 의 모델 ID>"
model_reasoning_effort = "medium"
```

## 개인 설정 끄기 (두 그룹 공통)

- `~/.codex/AGENTS.md` 등 전역 지침 파일은 실습 동안 비웁니다.
- 개인적으로 등록한 MCP 서버는 `codex mcp list` 로 확인하고 실습 동안 제거합니다 (`codex mcp remove <이름>`).

## 그룹 A: codegen MCP 연결

`npm run setup -- --group A --tool codex` 가 `codex mcp add` 를 실행합니다. 수동으로 하려면:

```bash
codex mcp add egovframe-codegen -- node <저장소 절대 경로>/.mcp-servers/egovframe-mcp-codegen/package/build/index.js
```

또는 `~/.codex/config.toml` 에 직접 추가합니다.

```toml
[mcp_servers.egovframe-codegen]
command = "node"
args = ["<저장소 절대 경로>/.mcp-servers/egovframe-mcp-codegen/package/build/index.js"]
```

연결 확인: `codex` 실행 후 `/mcp` 에서 `egovframe-codegen` 과 도구 네 개(`egov_scaffold_project`, `egov_add_entity`, `egov_add_query_view`, `egov_add_attach_file_infrastructure`)가 보이는지 확인합니다. 실습이 끝나면 `codex mcp remove egovframe-codegen` 으로 제거합니다.

## 그룹 B

`/mcp` 에 연결된 서버가 없어야 합니다.

## 작업 방법

- `board-app/` 에서 Codex 와 대화합니다. 파일 편집과 명령 실행 승인 방식은 평소 쓰던 대로 둡니다 (두 그룹 동일).
- 채점은 저장소 루트에서 `npm run verify`.
- 세션 기록은 `~/.codex/sessions/` 에 남으며, `npm run collect` 가 실습 시작 이후 생성된 파일의 사본을 모읍니다.
