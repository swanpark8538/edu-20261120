# Cursor 설정

## 모델 · 노력 수준

두 실행 모두 같은 모델을 씁니다. Cursor 는 여러 공급자 모델을 고를 수 있으므로, 구독에 따라 **Claude Opus 5.5** 또는 **GPT-6.1 Sol** 중 하나를 고르고 추론 노력(thinking/reasoning) 수준은 **medium** 에 해당하는 설정을 택합니다. 선택한 모델 이름을 진행자에게 알려 주세요 (기록 시트 `model` 열).

1. Cursor → Settings → Models 에서 사용할 모델 하나만 켭니다 (Auto 는 끕니다. 실행마다 모델이 바뀌면 비교가 어긋납니다).
2. 채팅 패널은 **Agent** 모드로 두고, 모델 선택기에서 같은 모델이 보이는지 확인합니다.

## 개인 설정 끄기

- Cursor Settings → Rules 의 User Rules, 작업 폴더의 `.cursor/rules/` 는 실습 동안 비웁니다.
- 전역 MCP(`~/.cursor/mcp.json`)에 개인 서버가 있으면 Settings → MCP 에서 실습 동안 끕니다.

## MCP 연결 (mcp 실행 폴더에만)

`npm run setup -- --tool cursor` 가 `~/egov-mcp-lab/<ID>/mcp/.cursor/mcp.json` 을 만들어 둡니다. `nomcp/` 에는 아무 설정도 없습니다.

```json
{
  "mcpServers": {
    "egovframe-codegen": {
      "command": "node",
      "args": ["<저장소 절대 경로>/.mcp-servers/egovframe-mcp-codegen/package/build/index.js"]
    }
  }
}
```

연결 확인: Cursor 로 **`mcp/` 폴더를 열면** 새 MCP 서버를 켤지 묻는 알림이 뜹니다 → **Enable**. Settings → MCP 에서 `egovframe-codegen` 이 초록색이고 도구 네 개(`egov_scaffold_project`, `egov_add_entity`, `egov_add_query_view`, `egov_add_attach_file_infrastructure`)가 보여야 합니다.
`nomcp/` 폴더를 열었을 때는 Settings → MCP 에 켜진 서버가 없어야 합니다.

Windows 에서 경로를 직접 적을 때는 `\` 를 `\\` 로 씁니다.

## 작업 방법

- Cursor 로 **작업 폴더**(`~/egov-mcp-lab/<ID>/mcp` 또는 `nomcp`)를 엽니다. 저장소 폴더(edu-20261120)는 열지 않습니다.
- Agent 채팅에서 `TASK.md` 대로 그 폴더 안에 `board-app/` 을 만들라고 요청합니다. 도구 호출 승인은 평소대로 합니다 (두 실행 동일).
- 두 번째 실행은 **다른 폴더를 새 창으로** 열어 처음부터 만듭니다.
- 채점은 저장소 루트에서 `npm run verify -- mcp` / `-- nomcp` (별도 터미널).
- Cursor 의 대화 기록은 내부 DB 에 저장되어 `npm run collect` 가 자동으로 모으지 못합니다. 진행자가 요청하면 채팅 패널의 내보내기 기능으로 전달합니다.
