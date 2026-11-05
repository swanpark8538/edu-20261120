# 표준프레임워크 기술 심화교육 · MCP 과목 실습 저장소

전자정부 표준프레임워크(eGovFrame) 5.0 Boot 프로젝트에 **첨부파일을 올릴 수 있는 게시판**을 추가하는 실습입니다.
AI 코딩 도구(Claude Code · Codex · Cline)와 함께 40분 안에 과제를 수행하고, 채점기로 진행 상황을 확인한 뒤 결과를 이 저장소에 올립니다.

| 구분 | 내용 |
|---|---|
| 기반 프로젝트 | `board-app/` — eGovFrame 5.0 Boot + 부서(Dept) CRUD 예제가 이미 들어 있음 |
| 과제 | `board-app/TASK.md` — 첨부파일 게시판(Board) 추가 |
| 채점 | `npm run verify` — 기본 → 기능 → 품질 → 표준 준수 16개 항목 ([docs/milestones.md](docs/milestones.md)) |
| 제출 | `submit/<참가자ID>` 브랜치에 커밋 후 push |

## 1. 사전 준비 (실습 전날까지)

| 도구 | 버전 | 확인 |
|---|---|---|
| JDK | 17 | `java -version` |
| Maven | 3.9 | `mvn -version` |
| Node.js | 22 (18 이상) | `node --version` |
| Git | 최신 | `git --version` |
| AI 코딩 도구 | 아래 중 하나 | Claude Code / Codex CLI / VS Code + Cline 확장 |

AI 도구와 모델은 다음 중 하나로 맞춥니다. 자세한 설정은 각 문서를 보세요.

| 사용 도구 | 모델 · 노력 수준 | 문서 |
|---|---|---|
| Claude Code | **Opus 5.5 · medium** | [docs/setup-claude-code.md](docs/setup-claude-code.md) |
| Codex | **GPT-6.1 Sol · medium** | [docs/setup-codex.md](docs/setup-codex.md) |
| 상용 AI 구독이 없는 경우 | VS Code **Cline** + OpenRouter(진행자가 API Key 제공) · **GPT-OSS-120B** | [docs/setup-cline.md](docs/setup-cline.md) |

## 2. 환경 구성 (clone 직후 1회)

```bash
git clone https://github.com/swanpark8538/edu-20261120.git
```

```bash
cd edu-20261120 && npm install
```

진행자가 알려 준 **참가자 ID**, **그룹(A/B)**, 사용할 **도구**를 넣어 실행합니다.

```bash
npm run setup -- --id P01 --group A --tool claude
```

- `--group A` 는 eGovFrame 코드 생성 MCP(`egovframe-codegen`)를 AI 도구에 연결하는 그룹, `--group B` 는 연결하지 않는 그룹입니다. 어느 그룹인지는 진행자가 정해 줍니다.
- `--tool` 은 `claude` / `codex` / `cline` 중 하나입니다. 그룹 A 라면 그 도구용 MCP 연결 설정을 만들어 주거나 붙여 넣을 내용을 출력합니다.
- 스크립트는 `mcp/` 의 MCP 서버 번들을 `.mcp-servers/` 에 풀고, 제출 브랜치 `submit/<ID>` 를 만들고, `board-app` 을 한 번 빌드해 Maven 의존성을 미리 받아 둡니다 (처음에는 몇 분 걸립니다).
- Windows 에서는 PowerShell 또는 Git Bash 에서 실행합니다. `tar` 가 필요합니다 (Windows 10 이상 기본 포함).

끝나면 한 번 채점해서 **B1(빌드)만 통과하는지** 확인합니다 (게시판이 아직 없으므로 나머지 15개는 실패가 정상입니다). 점검용으로 남은 `results/human/<ID>/progress.jsonl` 은 지웁니다.

```bash
npm run verify
```

## 3. 실습 진행

1. 진행자가 시작을 알리면 `board-app/TASK.md` 를 읽고 시작합니다. 제한 시간은 **40분**입니다.
2. AI 도구는 `board-app/` 디렉터리에서 실행합니다 (예: `cd board-app && claude`). IDE 로 직접 코드를 읽고 고쳐도 됩니다.
3. 진행 상황이 궁금하면 언제든 저장소 루트에서 `npm run verify` 를 실행합니다. 2~3분 걸리며, 실행 기록이 `results/human/<ID>/progress.jsonl` 에 쌓입니다. **단계를 끝냈다고 생각할 때마다 실행해 주세요.** 이 기록이 측정 자료가 됩니다.
4. 채점기는 프로젝트를 임시 폴더에 복사해서 빌드·실행하므로, IDE 에서 띄워 둔 앱과 충돌하지 않습니다.
5. 웹 검색과 공식 문서 열람은 허용됩니다. 다른 참가자와 대화하는 것은 허용되지 않습니다.
6. 진행자는 과제 내용에 관한 질문에 답하지 않습니다. PC·네트워크 장애만 손을 들어 알려 주세요.

## 4. 결과 제출 (종료 선언 후)

```bash
npm run collect
```

최종 채점 결과와 변경 규모가 `results/human/<ID>/` 에 저장되고, 커밋·push 명령이 출력됩니다. 그대로 실행하세요.

```bash
git add -A
```

```bash
git commit -m "submit: P01"
```

```bash
git push -u origin submit/P01
```

push 권한이 없으면 이 저장소를 fork 해서 fork 에 push 한 뒤 Pull Request 를 올립니다. AI 도구와 나눈 대화 기록 사본(`results/human/<ID>/transcripts/`)은 git 에 올라가지 않으며, 진행자가 요청할 때만 따로 전달합니다.

## 5. 디렉터리 구성

```
board-app/            실습 대상 프로젝트 (eGovFrame 5.0 Boot, Dept 예제 포함) — 여기서 작업
board-app/TASK.md     과제 요구사항 + 인수 조건
materials/            과제 DDL (board.ddl.sql, dept.ddl.sql), 요구사항 원본
mcp/                  MCP 서버 번들 (egovframe-mcp-codegen, egovframe-mcp-check)
scripts/
  setup.mjs           환경 구성          npm run setup -- --id ID --group A|B --tool claude|codex|cline
  verify.mjs          채점기             npm run verify
  collect.mjs         결과 수집          npm run collect
  make-reference.mjs  [진행자] 정답 프로젝트 생성   npm run reference
  aggregate-human.mjs [진행자] 결과 집계           npm run aggregate
  gather-submissions.sh [진행자] submit/* 브랜치에서 결과 모으기
config/               공통 설정 (kit.config.mjs, milestones.mjs)
docs/
  briefing.md         실습 전에 알아 두어야 할 것 (프로젝트 구조, 엔티티, URL 규칙, 패키지)
  milestones.md       채점 16개 항목의 판정 기준
  setup-claude-code.md / setup-codex.md / setup-cline.md   도구별 설정
  facilitator-guide.md, surveys.md, record-sheet.csv       진행자 자료
results/human/<ID>/   채점 기록·최종 결과 (제출 브랜치에 포함)
```

## 6. 자주 묻는 것

- **`npm run verify -- --static`** 을 붙이면 빌드·기동 없이 소스 정적 검사 항목(Q1·Q4·Q5·Q6 일부·S1)만 수십 초 안에 확인합니다. 기록은 남지 않습니다.
- **빌드가 느립니다.** 첫 빌드는 의존성을 내려받느라 몇 분 걸립니다. `npm run setup` 이 미리 받아 두므로 실습 당일에는 빠릅니다.
- **채점기가 "CSRF 토큰을 찾지 못해" 경고를 냅니다.** 게시판 화면에 `_csrf` meta 또는 hidden input 이 없다는 뜻입니다. 기존 Dept 화면을 참고하세요.
- **S1(표준 준수)이 "검사 실행 실패"입니다.** `.mcp-servers/egovframe-mcp-check` 가 없거나 이 PC 플랫폼용 번들이 없을 때입니다. `npm run setup` 을 다시 실행하거나 진행자에게 알려 주세요.
- **그룹 A 인데 MCP 가 연결되지 않습니다.** 각 도구 문서의 "연결 확인" 절을 보세요. `board-app/.mcp.json`(Claude Code) 의 경로가 이 PC 의 절대 경로인지 확인합니다.
