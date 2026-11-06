# 표준프레임워크 기술 심화교육 · MCP 과목 실습 저장소

AI 코딩 도구(Claude Code · Codex · Cline)로 **전자정부 표준프레임워크(eGovFrame) 5.0 Boot 첨부파일 게시판 프로젝트**를 빈 폴더에서 만드는 실습입니다.
수강생 전원이 **코드 생성 MCP 를 연결한 실행(실험군, 20분)** 과 **연결하지 않은 실행(대조군, 30분)** 을 한 번씩 수행하고, 채점기로 진행 상황을 기록한 뒤 결과를 이 저장소에 올립니다.

> **이 저장소는 채점 키트입니다. AI 도구의 작업 영역으로 열지 마세요.**
> 실습은 `npm run setup` 이 만들어 주는 저장소 밖 폴더(`~/egov-mcp-lab/<ID>/mcp`, `~/egov-mcp-lab/<ID>/nomcp`)에서 합니다.

| 구분 | 내용 |
|---|---|
| 과제 | 작업 폴더의 `TASK.md` (원본: `materials/task-requirements.md`) — 빈 폴더에 `board-app/` 프로젝트 생성 |
| 채점 | `npm run verify -- mcp` / `npm run verify -- nomcp` — 18개 항목 ([docs/milestones.md](docs/milestones.md)) |
| 제출 | `npm run collect` → `submissions/<ID>/` 를 `submit/<ID>` 브랜치에 커밋 후 push |

## 1. 사전 준비 (교육 전날까지)

| 도구 | 버전 | 확인 |
|---|---|---|
| JDK | 17 | `java -version` |
| Maven | 3.9 (3.9.9 권장) | `mvn -version` |
| Node.js | 22 | `node --version` |
| Git | 최신 | `git --version` |

AI 코딩 도구는 다음 중 하나를 준비하고, 모델과 노력 수준을 맞춥니다.

| 사용 도구 | 모델 · 노력 수준 | 문서 |
|---|---|---|
| Claude Code (권장) | **Opus 5.5 · medium** | [docs/setup-claude-code.md](docs/setup-claude-code.md) |
| Codex (권장) | **GPT-6.1 Sol · medium** | [docs/setup-codex.md](docs/setup-codex.md) |
| 상용 AI 를 쓸 수 없는 경우 | VS Code **Cline** + OpenRouter(API Key 는 실습 중 센터 제공) · **GPT-OSS-120B** | [docs/setup-cline.md](docs/setup-cline.md) |

## 2. 환경 구성 (clone 직후 1회, 교육 전날 권장)

```bash
git clone https://github.com/swanpark8538/edu-20261120.git
```

```bash
cd edu-20261120 && npm install
```

진행자가 알려 준 **참가자 ID** 와 사용할 **도구**, **실행 순서**를 넣어 실행합니다. 순서를 모르면 생략해도 됩니다 (ID 끝자리로 자동 결정).

```bash
npm run setup -- --id P01 --tool claude --order mcp-first
```

스크립트가 하는 일은 다음과 같습니다.

- `mcp/` 의 MCP 서버 번들을 `.mcp-servers/` 에 풉니다.
- 작업 영역 `~/egov-mcp-lab/P01/mcp/` 와 `~/egov-mcp-lab/P01/nomcp/` 를 만들고 각각 `TASK.md` 를 둡니다. `mcp/` 에만 AI 도구용 MCP 연결 설정이 들어갑니다.
- 제출 브랜치 `submit/P01` 로 전환합니다.
- codegen 으로 임시 프로젝트를 만들어 한 번 빌드해 Maven 의존성을 미리 받아 둡니다 (처음에는 몇 분 걸립니다. 임시 프로젝트는 지워집니다).

Windows 에서는 PowerShell 또는 Git Bash 에서 실행합니다 (`tar` 필요, Windows 10 이상 기본 포함). 다른 위치를 쓰려면 `--workspace <경로>` 를 주되 저장소 밖이어야 합니다.

끝나면 각 실행의 채점을 한 번씩 돌려 "프로젝트 없음" 으로 0/18 이 기록되는지 확인합니다. 이 기록은 점검용이므로 `submissions/P01/` 을 지웁니다.

```bash
npm run verify -- mcp
```

## 3. 실습 진행 (두 번 실행)

실행 순서는 setup 출력에 나옵니다. 각 실행은 다음 절차로 진행합니다.

1. **시작 직전** `npm run verify -- <mcp|nomcp>` 를 실행합니다. 프로젝트가 없으므로 0 점이지만 **시작 시각**이 기록됩니다.
2. AI 도구를 **해당 작업 폴더에서** 실행합니다 (예: `cd ~/egov-mcp-lab/P01/mcp && claude`). `TASK.md` 를 읽고 그 폴더 안에 `board-app/` 프로젝트를 만듭니다.
   - `mcp` 실행: eGovFrame 코드 생성 MCP(`egovframe-codegen`)가 연결되어 있습니다. Codex·Cline 은 전역 설정이므로 `npm run mcp -- on` / MCP 화면에서 켠 뒤 시작합니다.
   - `nomcp` 실행: MCP 없이 AI 도구의 기본 기능만 사용합니다. Codex 는 `npm run mcp -- off`, Cline 은 MCP 화면에서 끈 뒤 시작합니다.
3. **중간에 한 번쯤** `npm run verify -- <실행>` 으로 진척을 확인합니다 (2~3분 걸림. 실행마다 기록이 쌓여 도달 곡선 자료가 됩니다). `-- --static` 을 붙이면 빌드 없이 정적 항목만 빠르게 봅니다.
4. **종료 선언 직후** 다시 `npm run verify -- <실행>` 을 실행합니다 (종료 시각 기록).

시간: 실험군(mcp) **20분**, 대조군(nomcp) **30분**. 웹 검색과 공식 문서 열람은 허용되고, 다른 참가자와의 대화는 허용되지 않습니다. 진행자는 과제 내용 질문에 답하지 않습니다.

## 4. 결과 제출

두 실행이 끝나면:

```bash
npm run collect
```

실행별 최종 채점·산출물 사본·규모가 `submissions/P01/{mcp,nomcp}/` 에 저장되고, 커밋·push 명령이 출력됩니다. 그대로 실행하세요.

```bash
git add submissions/P01
```

```bash
git commit -m "submit: P01"
```

```bash
git push -u origin submit/P01
```

push 권한이 없으면 이 저장소를 fork 해서 fork 에 push 한 뒤 Pull Request 를 올립니다. 산출물 사본에서 정적 이미지·폰트는 제외되고 목록만 남습니다. AI 도구 대화 기록 사본(`transcripts/`)은 git 에 올라가지 않으며 진행자가 요청할 때만 전달합니다. 모집된 결과물은 센터에서 AI 로 점검합니다.

## 5. 디렉터리 구성

```
materials/task-requirements.md   과제 원본 (setup 이 작업 폴더에 TASK.md 로 복사)
materials/board.ddl.sql          과제 DDL
mcp/                             MCP 서버 번들 (egovframe-mcp-codegen, egovframe-mcp-check)
scripts/
  setup.mjs        환경 구성      npm run setup -- --id ID --tool claude|codex|cline [--order ...]
  verify.mjs       채점기         npm run verify -- mcp|nomcp [--static]
  collect.mjs      결과 수집      npm run collect
  mcp-toggle.mjs   전역 MCP 켜기/끄기 (Codex)   npm run mcp -- on|off
  make-reference.mjs      [진행자] 참조 프로젝트 생성   npm run reference
  aggregate-human.mjs     [진행자] 결과 집계           npm run aggregate
  gather-submissions.sh   [진행자] submit/* 브랜치에서 제출물 모으기
config/            공통 설정 (kit.config.mjs, milestones.mjs)
docs/              briefing.md(사전 안내) · milestones.md(판정 기준) · setup-*.md(도구별) · facilitator-guide.md · surveys.md · record-sheet.csv
submissions/<ID>/  제출물 (채점 기록, 산출물 사본)
```

## 6. 자주 묻는 것

- **AI 가 저장소의 채점 스크립트를 읽고 코드를 만듭니다.** 저장소를 작업 영역으로 열었기 때문입니다. AI 도구는 반드시 `~/egov-mcp-lab/<ID>/<실행>` 폴더에서 실행하세요.
- **빌드가 느립니다.** 첫 빌드는 의존성을 내려받느라 몇 분 걸립니다. `npm run setup` 이 미리 받아 두므로 당일에는 빠릅니다.
- **채점기가 "CSRF 토큰을 찾지 못했습니다" 라고 합니다.** 목록 화면에 `_csrf` meta 또는 hidden input 이 없다는 뜻입니다. 토큰이 없으면 모든 POST 가 거부되어 F1 이후가 실패합니다.
- **R1(표준 준수율)이 "검사 실행 실패"입니다.** `.mcp-servers/egovframe-mcp-check` 가 없거나 이 PC 플랫폼용 번들이 없을 때입니다. `npm run setup` 을 다시 실행하거나 진행자에게 알려 주세요.
- **mcp 실행인데 MCP 가 연결되지 않습니다.** 도구 문서의 "연결 확인" 절을 보세요. Claude Code 는 `mcp/.mcp.json` 의 경로가 이 PC 의 절대 경로인지 확인합니다.
