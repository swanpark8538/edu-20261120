# 실습 전에 알아 두어야 할 것 (사전 안내)

진행자가 설명 시간(약 15분)에 참가자 전원에게 같은 내용으로 전달합니다. 참가자는 이 문서를 실습 전날 읽어 두세요.
과제 자체(무엇을 어떻게 구현할지)에 관한 질문은 실습 중에 받지 않으므로, 궁금한 점은 이 문서와 `board-app/TASK.md` 에서 미리 확인합니다.

## 1. 기반 프로젝트 `board-app` 이 무엇인지

| 항목 | 내용 |
|---|---|
| 프레임워크 | 전자정부 표준프레임워크 **5.0 Boot** (`egovframe-boot-starter-parent 5.0.0`), Spring Boot 3 계열, **Java 17**, Jakarta EE |
| 빌드 | Maven (`mvn package` → `target/board-app-1.0.0.jar`) |
| 화면 | Thymeleaf + KRDS(정부 디자인 시스템) CSS. 템플릿은 `src/main/resources/templates/thymeleaf/` |
| DB | 내장 **HSQL** (메모리). 앱 기동 시 `src/main/resources/db/deptdb.sql` 이 실행되어 테이블이 만들어진다. 다른 DB 용 DDL 은 `db/migrations/<dbms>/` |
| 보안 | Spring Security — **CSRF 보호 켜져 있음**. 모든 POST 에 `_csrf` 토큰 필요. 화면에는 `EgovCsrfControllerAdvice` 가 넣어 주는 `${_csrf}` 를 meta/hidden 으로 출력 |
| 포트 | 8080 (`application.properties`). 채점기는 별도 포트로 따로 띄우므로 그대로 두어도 된다 |
| 실행 | `cd board-app && mvn spring-boot:run` 또는 `mvn package && java -jar target/board-app-1.0.0.jar` → `http://localhost:8080/egovDeptList.do` |

## 2. 이미 구현된 예제 엔티티: Dept (부서)

과제의 Board 는 **Dept 와 같은 방식**으로 만들면 됩니다. Dept 가 곧 "이 프로젝트의 표준 구현 예시"입니다.

### DDL (`materials/dept.ddl.sql`)

```sql
CREATE TABLE DEPT (
  DEPT_ID   VARCHAR(20) NOT NULL,
  DEPT_NM   VARCHAR(100) NOT NULL,
  USE_YN    CHAR(1) NOT NULL,
  PRIMARY KEY (DEPT_ID)
);
```

ID 채번용 테이블 `IDS(TABLE_NAME, NEXT_ID)` 도 시드 SQL 에 함께 있습니다 (`INSERT INTO IDS VALUES('DEPT', 1)`).

### 패키지와 파일 (base package `egovframework.example`)

```
egovframework.example
├─ EgovBootApplication.java                 Spring Boot 진입점
├─ config/                                  @Configuration 자바 설정 (XML 없음)
│   ├─ EgovConfigDatasource / Mapper / Common / Properties / Web / WebSecurity
│   ├─ EgovConfigDeptIdGeneration.java      ← 엔티티별 ID 생성기 빈 (prefix "DEPT-", 5자리, IDS 테이블)
│   ├─ EgovConfigTransaction.java           ← 트랜잭션 포인트컷: execution(* egovframework.example.dept..impl.*Impl.*(..))
│   ├─ EgovConfigAspect.java                ← 예외 전환 AOP (egovframework.example..impl.*Impl.*)
│   └─ EgovConfigValidation.java            ← Bean Validation + 메시지 소스
├─ dept/
│   ├─ web/EgovDeptController.java          @Controller, URL 매핑
│   ├─ service/EgovDeptService.java         서비스 인터페이스
│   ├─ service/DeptVO.java                  VO (+ @EgovNullCheck 검증 어노테이션)
│   ├─ service/DeptDefaultVO.java           검색·페이징 공통 필드 (searchCondition, searchKeyword, pageIndex, pageUnit, firstIndex, recordCountPerPage …)
│   └─ service/impl/EgovDeptServiceImpl.java  @Service("deptService"), extends EgovAbstractServiceImpl
│       service/impl/DeptMapper.java          @Mapper("deptMapper") MyBatis 매퍼 인터페이스
├─ cmm/                                     공통 (CSRF 거부 컨트롤러 등)
├─ exception/                               EgovAopExceptionTransfer, 예외 핸들러
└─ pagination/                              KRDS 페이징 렌더러

src/main/resources
├─ application.properties, application-hsql.properties
├─ db/deptdb.sql                            HSQL 시드 (테이블 + IDS 초기값)
├─ egovframework/sqlmap/dept/mappers/EgovDept_Dept_SQL.xml   MyBatis SQL (LIKE 검색, LIMIT/OFFSET 페이징)
├─ egovframework/message/message-common.properties / _ko / _en   메시지 번들 3종
└─ templates/thymeleaf/dept/egovDeptList.html, egovDeptRegister.html, _fragments/nav.html
```

### URL 규칙 (Dept 기준 — Board 도 같은 규칙)

| 기능 | Dept | Board (과제) |
|---|---|---|
| 목록 | `GET /egovDeptList.do` | `GET /egovBoardList.do` |
| 등록 화면 | `POST /addDeptView.do` | `POST /addBoardView.do` |
| 등록 처리 | `POST /addDept.do` | `POST /addBoard.do` (multipart) |
| 수정 화면 | `POST /updateDeptView.do` (`deptId`) | `POST /updateBoardView.do` (`boardId`) |
| 수정 처리 | `POST /updateDept.do` | `POST /updateBoard.do` (multipart) |
| 삭제 | `POST /deleteDept.do` (`deptId`) | `POST /deleteBoard.do` (`boardId`) |
| 목록 파라미터 | `pageIndex`, `searchCondition`(0=이름), `searchKeyword` | 같음 (searchCondition 0/1/2 = 제목/내용/작성자 권장) |
| 다국어 전환 | `?language=en` | 같음 |

### Dept 에서 눈여겨볼 "표준" 요소

- **계층**: Controller → Service(인터페이스) → ServiceImpl(`EgovAbstractServiceImpl` 상속) → Mapper(`@Mapper`). 표준 준수 검사(S1)는 이 네 가지 패턴을 봅니다.
- **ID 생성**: `EgovIdGnrService.getNextStringId()` 를 ServiceImpl 에서 호출. 생성기 빈은 `config/EgovConfig<Entity>IdGeneration.java`.
- **트랜잭션**: `EgovConfigTransaction` 의 `txAdvisor` 포인트컷이 `egovframework.example.dept..impl.*Impl.*` 로 **Dept 패키지에 한정**되어 있습니다. 새 엔티티를 추가하면 범위를 넓히거나 포인트컷을 추가해야 합니다.
- **검증**: VO 필드의 `@EgovNullCheck(message="{confirm.required.dept.deptNm}")` + 컨트롤러 `@Valid` + `BindingResult`.
- **다국어**: 화면 문자열은 `#{title.dept.deptNm}` 식 메시지 키. 키는 번들 3종(`message-common.properties`, `_ko`, `_en`)에 모두 추가.
- **예외**: `processException("info.nodata.msg")` → `EgovBizException` → `EgovConfigWeb` 의 `SimpleMappingExceptionResolver` 가 안내 화면(`dept/egovSampleError`)으로 처리.
- **페이징**: `PaginationInfo` + `DeptDefaultVO` 의 `firstIndex`/`recordCountPerPage` → SQL `LIMIT #{recordCountPerPage} OFFSET #{firstIndex}`. 페이지 크기는 `EgovConfigProperties` 의 `pageUnit=10`.

## 3. 과제 엔티티: Board (게시판)

- **DDL**: `materials/board.ddl.sql` (BOARD_ID VARCHAR(20) PK, TITLE, CONTENT, WRITER). 첨부 연결용 컬럼·테이블은 자유롭게 추가.
- **패키지**: `egovframework.example.board` 권장 (web / service / service.impl).
- **첨부파일**: 등록·수정 폼의 파일 필드 이름은 `file_1`(multiple), 첨부 그룹 키는 `atchFileId`. 파일당 10MB, exe 등 실행 파일 차단, 초과·차단 시 **안내 화면**(5xx 금지). 다운로드는 `<a href>` 링크, 응답 헤더 `Content-Disposition: attachment`.
- **ID**: 사용자가 입력하지 않고 서버가 채번 (등록 화면에 ID 입력란 없음).
- 나머지 인수 조건은 `board-app/TASK.md` 의 표를 그대로 지켜야 채점기가 인식합니다.

## 4. 채점 (`npm run verify`)

| 구분 | 항목 | 비고 |
|---|---|---|
| 기본 | B1 빌드, B2 기동 | 실패하면 이후 기능 항목은 모두 건너뜀 |
| 기능 | F1 등록 → F2 조회 → F3 수정 → F4 삭제 → F5 업로드(2MB) → F6 다운로드 → F7 업로드 보안(exe, 11MB) | 앞 단계가 실패하면 뒤는 건너뜀 |
| 품질 | Q1 Config 클래스, Q2 페이징(12건), Q3 검색, Q4 트랜잭션, Q5 ID 생성, Q6 예외/검증/i18n | 정적 분석 + HTTP 검사 |
| 표준 준수 | S1 표준프레임워크 적용률 | `egovframe-mcp-check` 가 Controller/ServiceImpl/Mapper 패턴을 검사. 100% + Board 계층 파일 3개 이상 |

자세한 판정 기준은 [milestones.md](milestones.md). 채점에 2~3분 걸리며, **실행할 때마다 기록이 남아 도달 곡선 자료가 됩니다.** 단계를 끝냈다고 생각할 때마다 실행하세요.

## 5. AI 도구와 MCP

- 도구·모델은 참가자가 가진 것에 따라 다르지만 **노력 수준은 모두 medium** 으로 맞춥니다: Claude Code → Opus 5.5, Codex → GPT-6.1 Sol, Cline(OpenRouter) → GPT-OSS-120B.
- **그룹 A** 는 `egovframe-codegen` MCP 가 연결되어 있습니다. 이 서버는 네 개의 도구를 제공합니다: `egov_scaffold_project`(새 프로젝트), `egov_add_entity`(기존 프로젝트에 엔티티 CRUD 추가, `withAttachment` 옵션), `egov_add_query_view`(조회 화면), `egov_add_attach_file_infrastructure`(첨부파일 인프라 설치). AI 에게 자연어로 요청하면 필요한 도구를 골라 호출합니다. 도구 호출 전에 권한을 물으면 허용하세요.
- **그룹 B** 는 MCP 없이 AI 도구의 기본 기능(파일 읽기·편집, 명령 실행)만 사용합니다.
- 두 그룹 모두 개인 설정(전역 CLAUDE.md, 개인 스킬·플러그인, 개인적으로 등록한 MCP 서버)은 실습 동안 끕니다.

## 6. 하지 말아야 할 것

- Dept 기능·파일을 삭제하거나 깨뜨리는 것 (채점기가 CSRF 토큰을 Dept 화면에서 빌릴 수 있습니다).
- `server.port`, `spring.profiles.active`(hsql), CSRF 설정을 바꾸는 것.
- `board-app/` 밖(스크립트, 설정)을 수정하는 것.
- 다른 참가자와 대화하거나 화면을 공유하는 것.

## 7. 시간표

| 시간 | 내용 |
|---|---|
| 설명 15분 | 이 문서 요약 + 환경 점검 + TASK.md 읽기 (질문은 이때만) |
| **40분** | 과제 수행. 10분 남았을 때 안내 |
| 5분 | `npm run collect` → 커밋·push → 사후 설문 |
