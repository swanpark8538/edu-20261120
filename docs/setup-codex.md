# Codex 설정

## 모델 · 노력 수준

두 실행 모두 **GPT-6.1 Sol, 추론 노력(reasoning effort) medium** 으로 맞춥니다.

1. 작업 폴더에서 `codex` 를 실행합니다.
2. `/model` 로 **GPT-6.1 Sol** 을 선택하고 추론 노력을 **medium** 으로 둡니다.
3. 설정 파일로 고정하려면 `~/.codex/config.toml` 에 다음을 둡니다 (모델 ID 는 `/model` 화면의 값을 그대로. 실습 후 복원).

```toml
model = "<GPT-6.1 Sol 의 모델 ID>"
model_reasoning_effort = "medium"
```

## 개인 설정 끄기

- `~/.codex/AGENTS.md` 등 전역 지침은 실습 동안 비웁니다.
- 개인적으로 등록한 MCP 서버는 `codex mcp list` 로 확인하고 실습 동안 제거합니다.

## MCP 켜기/끄기 (전역 설정이므로 실행마다 전환)

Codex 의 MCP 설정은 `~/.codex/config.toml` 전역입니다. 따라서 **mcp 실행 직전에 켜고, nomcp 실행 직전에 꺼야** 합니다.

```bash
npm run mcp -- on
```

```bash
npm run mcp -- off
```

각각 `codex mcp add egovframe-codegen -- node <codegen 경로>` / `codex mcp remove egovframe-codegen` 을 실행합니다. 수동으로 하려면 `~/.codex/config.toml` 에 다음 블록을 넣거나 지웁니다.

```toml
[mcp_servers.egovframe-codegen]
command = "node"
args = ["<저장소 절대 경로>/.mcp-servers/egovframe-mcp-codegen/package/build/index.js"]
```

전환 후 `codex` 를 다시 시작하고 `/mcp` 로 확인합니다: mcp 실행에서는 `egovframe-codegen` 과 도구 네 개가 보여야 하고, nomcp 실행에서는 비어 있어야 합니다. 실습이 끝나면 `npm run mcp -- off` 로 제거합니다.

## 작업 방법

- 작업 폴더(`~/egov-mcp-lab/<ID>/mcp` 또는 `nomcp`)에서 `codex` 를 실행하고 `TASK.md` 대로 `board-app/` 을 만듭니다. 파일 편집·명령 승인 방식은 평소대로 둡니다 (두 실행 동일).
- 채점은 저장소 루트에서 `npm run verify -- mcp` / `-- nomcp`.
- 세션 기록은 `~/.codex/sessions/` 에 남으며 `npm run collect` 가 작업 폴더 경로가 든 기록의 사본을 모읍니다.
