# Claude Code 설정

## 모델 · 노력 수준

두 실행 모두 **Claude Opus 5.5, 노력 수준(effort) medium** 으로 맞춥니다.

1. 작업 폴더(`~/egov-mcp-lab/<ID>/mcp` 또는 `nomcp`)에서 `claude` 를 실행합니다.
2. `/model` 로 **Opus 5.5** 를 선택하고, 같은 화면에서 노력 수준을 **medium** 으로 둡니다.
3. 상태 표시줄 또는 `/status` 로 확인합니다.

설정 파일로 고정하려면 `~/.claude/settings.json` 에 다음을 둡니다 (실습 후 복원).

```json
{
  "model": "opus",
  "effortLevel": "medium"
}
```

## 개인 설정 끄기

- `~/.claude/CLAUDE.md`(전역 지침), `~/.claude/skills/`, 개인 플러그인은 실습 동안 비웁니다.
- 사용자 범위 MCP 서버는 `claude mcp list` 로 확인하고 실습 동안 제거합니다.

## MCP 연결 (mcp 실행 폴더에만)

`npm run setup -- --tool claude` 가 `~/egov-mcp-lab/<ID>/mcp/.mcp.json` 과 `.claude/settings.local.json` 을 만들어 둡니다. `nomcp/` 에는 아무 설정도 없습니다.

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

연결 확인: `mcp/` 에서 `claude` → `/mcp` → `egovframe-codegen` 이 **connected**, 도구 네 개(`egov_scaffold_project`, `egov_add_entity`, `egov_add_query_view`, `egov_add_attach_file_infrastructure`)가 보여야 합니다. 프로젝트 MCP 서버 사용 여부를 물으면 허용합니다.
`nomcp/` 에서 `claude` → `/mcp` 에 연결된 서버가 없어야 합니다.

## 작업 방법

- 작업 폴더에서 Claude 와 대화합니다. "TASK.md 를 읽고 이 폴더에 board-app 프로젝트를 만들어 줘" 처럼 시작해도 됩니다.
- Claude 가 `board-app/` 을 만들면 그 안으로 들어가(`cd board-app`) 계속해도 되고, 작업 폴더에서 계속해도 됩니다.
- 채점은 저장소 루트에서 `npm run verify -- mcp` / `-- nomcp`.
- 대화 기록은 `~/.claude/projects/` 에 남으며 `npm run collect` 가 사본을 모읍니다 (git 제외).
