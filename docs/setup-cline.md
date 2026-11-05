# VS Code + Cline + OpenRouter 설정

상용 AI 구독(Claude·ChatGPT 등)이 없는 참가자용입니다. 진행자가 **OpenRouter API Key** 를 제공하며, 모델은 **GPT-OSS-120B** 를 사용합니다.

## 설치

1. VS Code 를 설치합니다.
2. 확장 마켓플레이스에서 **Cline** (saoudrizwan.claude-dev) 을 설치합니다.
3. VS Code 로 **저장소 루트가 아니라 `board-app/` 폴더**를 엽니다 (Cline 의 작업 디렉터리가 프로젝트 루트가 되도록).

## 모델 설정

Cline 패널 → 설정(⚙) → API Configuration:

| 항목 | 값 |
|---|---|
| API Provider | **OpenRouter** |
| OpenRouter API Key | 진행자가 배포한 키 (실습 후 진행자가 폐기합니다. 다른 곳에 저장하지 마세요) |
| Model | `openai/gpt-oss-120b` |
| Plan / Act 모드 | 둘 다 같은 모델을 사용 |

노력 수준 조절 항목이 보이면 **medium** 으로 둡니다 (없으면 기본값 그대로).

## 개인 설정 끄기 (두 그룹 공통)

- `.clinerules`, 전역 Custom Instructions 는 실습 동안 비웁니다.
- 개인적으로 등록한 MCP 서버는 Cline 의 MCP Servers 화면에서 실습 동안 끕니다.

## 그룹 A: codegen MCP 연결

`npm run setup -- --group A --tool cline` 이 붙여 넣을 JSON 과 설정 파일 위치를 출력합니다.

Cline 패널 → **MCP Servers** 아이콘 → **Configure MCP Servers** (파일 `cline_mcp_settings.json` 이 열림) → `mcpServers` 안에 추가:

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

Windows 에서는 경로의 `\` 를 `\\` 로 적습니다 (예: `C:\\edu\\edu-20261120\\.mcp-servers\\...`).

연결 확인: MCP Servers 목록에서 `egovframe-codegen` 이 초록색(연결됨)이고 도구 네 개(`egov_scaffold_project`, `egov_add_entity`, `egov_add_query_view`, `egov_add_attach_file_infrastructure`)가 펼쳐지는지 확인합니다.

## 그룹 B

MCP Servers 목록이 비어 있거나 모두 꺼져 있어야 합니다.

## 작업 방법

- Cline 채팅창에서 자연어로 요청합니다. 파일 편집·명령 실행 승인은 평소대로 합니다.
- 터미널에서 빌드·실행하거나 Cline 에게 맡겨도 됩니다. 채점은 저장소 루트에서 `npm run verify` (VS Code 통합 터미널에서 `cd ..` 후 실행).
- 작업 기록은 VS Code 전역 저장소의 Cline `tasks/` 폴더에 남으며, `npm run collect` 가 실습 시작 이후 파일의 사본을 모읍니다.
