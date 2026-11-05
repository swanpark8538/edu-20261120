# Claude Code 설정

## 모델 · 노력 수준

실습에서는 **Claude Opus 5.5, 노력 수준(effort) medium** 으로 맞춥니다.

1. `board-app/` 에서 `claude` 를 실행합니다.
2. `/model` 을 입력해 **Opus 5.5** 를 선택하고, 같은 화면에서 노력 수준을 **medium** 으로 둡니다 (Opus 계열은 모델 선택 화면에서 노력 수준을 함께 고릅니다).
3. 상태 표시줄 또는 `/status` 로 모델과 노력 수준을 확인합니다.

설정 파일로 고정하려면 `~/.claude/settings.json` 에 다음을 둡니다. 실습이 끝난 뒤 원래 값으로 되돌리세요.

```json
{
  "model": "opus",
  "effortLevel": "medium"
}
```

## 개인 설정 끄기 (두 그룹 공통)

측정이 개인 환경에 영향을 받지 않도록 실습 동안 아래를 비웁니다 (끝난 뒤 복원).

- `~/.claude/CLAUDE.md` (전역 지침), `~/.claude/skills/`, 개인 플러그인
- 사용자 범위로 등록한 MCP 서버: `claude mcp list` 로 확인하고 실습 동안 제거하거나 비활성화

## 그룹 A: codegen MCP 연결

`npm run setup -- --group A --tool claude` 가 `board-app/.mcp.json` 과 `board-app/.claude/settings.local.json` 을 만들어 둡니다.

```json
{
  "mcpServers": {
    "egovframe-codegen": {
      "type": "stdio",
      "command": "node",
      "args": ["<저장소 절대 경로>/.mcp-servers/egovframe-mcp-codegen/package/build/index.js"]
    }
  }
}
```

연결 확인: `board-app/` 에서 `claude` 실행 → `/mcp` → `egovframe-codegen` 이 **connected** 인지 확인합니다. 도구 목록에 `egov_scaffold_project`, `egov_add_entity`, `egov_add_query_view`, `egov_add_attach_file_infrastructure` 네 개가 보여야 합니다.

프로젝트 MCP 서버 사용 여부를 묻는 안내가 나오면 허용합니다. 도구 호출 권한을 물으면 허용합니다.

## 그룹 B

`board-app/` 에 `.mcp.json` 이 없어야 하며 `/mcp` 에 연결된 서버가 없어야 합니다.

## 작업 방법

- `board-app/` 에서 Claude 와 대화합니다. 요구사항은 `TASK.md` 에 있으니 "TASK.md 를 읽고 구현해 줘" 처럼 시작해도 됩니다.
- 빌드·실행을 Claude 에게 맡겨도 되고 직접 해도 됩니다. 채점은 저장소 루트에서 `npm run verify`.
- 대화 기록은 `~/.claude/projects/` 아래에 남으며, `npm run collect` 가 사본을 `results/human/<ID>/transcripts/` 에 모읍니다 (git 에는 올라가지 않음).
