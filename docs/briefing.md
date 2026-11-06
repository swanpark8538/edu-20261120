# 실습 전에 알아 두어야 할 것 (사전 안내)

진행자가 "실습 진행 방식 소개" 시간에 전원에게 같은 내용으로 전달합니다. 참가자는 교육 전날 읽어 두세요.
과제 자체에 관한 질문은 실습 중에 받지 않으므로, 궁금한 점은 이 문서와 작업 폴더의 `TASK.md` 에서 미리 확인합니다.

## 1. 무엇을 만드는가

**빈 폴더에서** 전자정부 표준프레임워크(eGovFrame) **5.0 Boot** 게시판 프로젝트 `board-app` 을 만듭니다. 기존 코드는 없습니다. 프로젝트 골격, 설정 클래스, 게시판 CRUD, 첨부파일, 페이징·검색, 보안까지 모두 이번 실습에서 만들어야 합니다.

| 항목 | 조건 |
|---|---|
| 스택 | eGovFrame 5.0 Boot (Spring Boot 3, Jakarta), Java 17, Maven, Thymeleaf + **KRDS** |
| DB | 내장 HSQL(메모리). 기동 시 테이블 자동 생성 (시드 SQL) |
| 보안 | Spring Security, CSRF 보호 켜짐, 보안 응답 헤더 |
| 실행 | `java -jar target/*.jar --server.port=… --Globals.fileStorePath=…` 로 기동되어야 함 |

### 과제 엔티티 Board

```sql
CREATE TABLE BOARD (
  BOARD_ID VARCHAR(20) NOT NULL,
  TITLE    VARCHAR(200) NOT NULL,
  CONTENT  VARCHAR(4000),
  WRITER   VARCHAR(50) NOT NULL,
  PRIMARY KEY (BOARD_ID)
);
```

- 패키지 권장: `egovframework.example.board`.
- 첨부: 폼 파일 필드 `file_1`(multiple), 첨부 그룹 키 `atchFileId`, 파일당 10MB, 실행 파일 차단, 초과·차단 시 안내 화면.
- 글 번호: 사용자가 입력하지 않고 서버가 자동으로 부여.

### URL · 파라미터 계약 (채점기가 그대로 호출)

| 기능 | 요청 | 파라미터 |
|---|---|---|
| 목록 | `GET /egovBoardList.do` | `pageIndex`, `searchCondition`(0 제목/1 내용/2 작성자), `searchKeyword` |
| 등록 화면 | `POST /addBoardView.do` | |
| 등록 처리 | `POST /addBoard.do` (multipart) | `title`, `content`, `writer`, `file_1` |
| 수정 화면 | `POST /updateBoardView.do` | `boardId` |
| 수정 처리 | `POST /updateBoard.do` (multipart) | `boardId`, `title`, `content`, `writer`, `atchFileId`, `file_1` |
| 삭제 | `POST /deleteBoard.do` | `boardId` |

모든 POST 에 `_csrf` 토큰이 필요하며, 목록 화면에 토큰이 meta 또는 hidden 으로 들어 있어야 채점기가 읽을 수 있습니다.

## 2. "표준프레임워크 방식" 이 무엇을 뜻하는가

`TASK.md` 는 발주서처럼 **무엇을** 만들지와 화면·요청 계약만 적고, **어떻게** 만들지는 적지 않습니다. 채점의 시스템 설정·보안·표준 준수율 항목은 결과물이 eGovFrame Boot 템플릿 프로젝트와 공통컴포넌트가 쓰는 방식(계층 구조, 자바 설정, 실행환경 공통 기반 서비스, 표준 보안 설정)을 따랐는지를 봅니다. 이 내용은 앞 시간 강의에서 다루며, 항목별 판정 기준은 실습이 끝난 뒤에 공개합니다.

실습에서는 평소 일하듯 AI 에게 요구를 전달하고 결과를 확인하면 됩니다. 채점 기준이나 표준 방식 설명을 AI 에게 붙여 넣는 것은 측정 취지에 맞지 않으므로 하지 않습니다.

## 3. 두 번 실행한다

| 실행 | 조건 | 시간 | 작업 폴더 |
|---|---|---|---|
| **mcp** (실험군) | eGovFrame 코드 생성 MCP `egovframe-codegen` 연결 | 20분 | `~/egov-mcp-lab/<ID>/mcp/` |
| **nomcp** (대조군) | MCP 없음, AI 도구 기본 기능만 | 30분 | `~/egov-mcp-lab/<ID>/nomcp/` |

- 순서는 참가자마다 다릅니다 (setup 출력 참고). 두 실행은 **서로 다른 폴더**에서 처음부터 다시 만듭니다. 첫 실행의 코드를 두 번째 폴더로 복사하지 않습니다.
- AI 도구는 반드시 해당 작업 폴더에서 실행합니다. **저장소(edu-20261120) 폴더를 열지 않습니다.** 저장소에는 채점 스크립트와 판정 기준이 있어 AI 가 그것을 읽으면 측정이 무의미해집니다.
- 도구·모델은 두 실행에서 같아야 합니다: Claude Code → Opus 5.5 medium, Codex → GPT-6.1 Sol medium, Cursor → Opus 5.5 또는 GPT-6.1 Sol medium(하나로 고정), Cline(OpenRouter) → GPT-OSS-120B.

### mcp 실행에서 쓸 수 있는 도구

`egovframe-codegen` 서버는 네 개의 도구를 제공합니다. AI 에게 자연어로 요청하면 필요한 도구를 골라 호출합니다. 도구 호출 권한을 물으면 허용하세요.

| 도구 | 용도 |
|---|---|
| `egov_scaffold_project` | DDL 로 eGovFrame 5.0 Boot 프로젝트 전체 골격 + 엔티티 CRUD 생성 (설정 클래스·KRDS 포함) |
| `egov_add_attach_file_infrastructure` | 기존 프로젝트에 표준 첨부파일 인프라(테이블·서비스·다운로드·보안 정책) 설치 |
| `egov_add_entity` | 기존 프로젝트에 엔티티 CRUD 추가 (`withAttachment` 옵션) |
| `egov_add_query_view` | 조회 전용 화면 추가 |

도구가 만든 코드와 과제 요구사항의 차이(예: 첨부 연결)는 직접 또는 AI 와 함께 보완합니다.

## 4. 채점 (`npm run verify -- mcp|nomcp`)

18개 항목을 여섯 구분으로 검사합니다. 항목별 판정 기준은 실습 뒤에 공개합니다.

| 구분 | 내용 | 항목 수 |
|---|---|---|
| 기본 동작 | 빌드, 기동 | 2 |
| 기본 기능 | 등록 → 조회 → 수정 → 삭제 → 첨부 업로드 → 다운로드 | 6 |
| 사용자 편의 | 페이징, 검색, KRDS | 3 |
| 시스템 설정 | 표준프레임워크 방식의 설정·공통 기반 서비스 적용 | 4 |
| 보안 | 첨부파일 보안, 웹 보안 | 2 |
| 표준 준수율 | `egovframe-mcp-check` 적용률 | 1 |

- 2~3분 걸립니다. **시작 직전과 종료 직후는 필수**, 중간은 1회 권장. 실행마다 기록이 남아 시간대별 점수 자료가 됩니다.
- 프로젝트가 아직 없으면 0점과 시각만 기록합니다 (시작 시각 용도).
- 채점기는 프로젝트를 임시 폴더에 복사해 빌드·기동하므로 띄워 둔 앱과 충돌하지 않습니다.
- 가중치를 적용한 최종 점수는 센터가 별도로 계산합니다.

## 5. 하지 말아야 할 것

- 저장소 폴더를 AI 작업 영역으로 여는 것, 채점 스크립트·판정 기준·이 문서의 설명을 AI 에게 보여 주는 것.
- 첫 실행의 코드를 두 번째 실행에 복사하는 것.
- nomcp 실행에서 MCP 를 켠 채로 작업하는 것 (Codex 는 `npm run mcp -- off`, Cline 은 MCP 화면에서 끄기, Cursor 는 nomcp 폴더를 열고 전역 서버 끄기).
- 외부 DB 를 요구하는 설정, `Globals.fileStorePath` 를 무시하는 저장 경로.
- 다른 참가자와 대화하거나 화면을 공유하는 것.

## 6. 당일 시간표 (총 2시간 20분)

| 시간 | 내용 |
|---|---|
| 30분 | 표준프레임워크 코드 생성 MCP 소개 |
| 20분 | 환경 구성 확인 (`npm run setup` 결과, 도구·모델, MCP 연결) |
| 10분 | 휴게 |
| 5분 | 실습 진행 방식 소개 (이 문서 요약) |
| 50분 | 실습: 첫 실행 → 폴더·MCP 전환 → 두 번째 실행 (mcp 20분 + nomcp 30분) |
| 5분 | `npm run collect` → 커밋·push |
| 20분 | 질의응답, 설문 |
