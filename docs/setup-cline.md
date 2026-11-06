# VS Code + Cline + OpenRouter 설정

상용 AI 를 쓸 수 없는 참가자용입니다. **OpenRouter API Key** 는 실습 동안 표준프레임워크 센터가 제공하고 실습 후 폐기합니다. 모델은 **GPT-OSS-120B** 입니다.

## 설치

1. VS Code 와 확장 **Cline**(saoudrizwan.claude-dev)을 설치합니다.
2. VS Code 로 **작업 폴더**(`~/egov-mcp-lab/<ID>/mcp` 또는 `nomcp`)를 엽니다. 저장소 폴더(edu-20261120)는 열지 않습니다.

## 모델 설정

Cline 패널 → 설정(⚙) → API Configuration:

| 항목 | 값 |
|---|---|
| API Provider | **OpenRouter** |
| OpenRouter API Key | 센터가 배포한 키 (다른 곳에 저장하지 마세요) |
| Model | `openai/gpt-oss-120b` |
| Plan / Act 모드 | 둘 다 같은 모델 |

노력(reasoning) 수준 항목이 보이면 **medium**, 없으면 기본값.

## 개인 설정 끄기

- `.clinerules`, 전역 Custom Instructions 는 실습 동안 비웁니다.
- 개인적으로 등록한 MCP 서버는 MCP Servers 화면에서 실습 동안 끕니다.

## MCP 켜기/끄기 (전역 설정이므로 실행마다 전환)

`npm run setup -- --tool cline` 이 붙여 넣을 JSON 과 설정 파일 위치를 출력합니다. Cline 패널 → **MCP Servers** → **Configure MCP Servers** (`cline_mcp_settings.json`) 의 `mcpServers` 에 추가합니다.

```json
{
  "mcpServers": {
    "egovframe-codegen": {
      "command": "node",
      "args": ["<저장소 절대 경로>/.mcp-servers/egovframe-mcp-codegen/package/build/index.js"],
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

Windows 에서는 경로의 `\` 를 `\\` 로 적습니다.

- **mcp 실행 직전**: MCP Servers 화면에서 `egovframe-codegen` 을 켭니다(초록색, 도구 네 개 표시).
- **nomcp 실행 직전**: 같은 화면에서 끕니다(토글 또는 `"disabled": true`). 목록이 모두 꺼져 있어야 합니다.

## 작업 방법

- Cline 채팅창에서 `TASK.md` 대로 `board-app/` 을 만들라고 요청합니다. 승인은 평소대로 합니다.
- 채점은 저장소 루트에서 `npm run verify -- mcp` / `-- nomcp` (별도 터미널).
- 작업 기록은 VS Code 전역 저장소의 Cline `tasks/` 폴더에 남으며 `npm run collect` 가 실습 시작 이후 파일의 사본을 모읍니다.
