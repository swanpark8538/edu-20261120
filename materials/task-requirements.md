# 과제: 첨부파일을 올릴 수 있는 게시판 프로젝트 만들기

이 폴더 안에 **전자정부 표준프레임워크(eGovFrame) 5.0 Boot 기반 게시판 프로젝트**를 새로 만드세요.
프로젝트 폴더 이름은 `board-app` 이어야 합니다 (이 폴더 바로 아래 `board-app/pom.xml` 이 생기도록).

## 기술 조건

| 항목 | 조건 |
|---|---|
| 프레임워크 | eGovFrame **5.0 Boot** (`org.egovframe.boot:egovframe-boot-starter-parent:5.0.0`), Spring Boot 3 계열 |
| 언어 · 빌드 | Java 17, Maven. `mvn -DskipTests package` 로 실행 가능한 jar 가 만들어져야 함 |
| 화면 | Thymeleaf. **KRDS(정부 디자인 시스템)** 스타일을 적용 |
| DB | **내장 HSQL(메모리)**. 앱 기동 시 BOARD 테이블과 첨부파일·ID 채번에 필요한 테이블이 자동으로 만들어져야 함. 외부 DB 를 요구하면 채점이 불가능함 |
| 보안 | Spring Security 로 **CSRF 보호 활성**, 보안 응답 헤더(`X-Content-Type-Options: nosniff`, `X-Frame-Options`) 설정 |
| 실행 | `java -jar target/*.jar --server.port=<포트> --Globals.fileStorePath=<경로>` 로 기동되어야 함 (채점기가 이 두 인자를 넘김). 첨부파일 저장 경로는 `Globals.fileStorePath` 프로퍼티에서 읽음 |

## 테이블

```sql
CREATE TABLE BOARD (
  BOARD_ID VARCHAR(20) NOT NULL,
  TITLE    VARCHAR(200) NOT NULL,
  CONTENT  VARCHAR(4000),
  WRITER   VARCHAR(50) NOT NULL,
  PRIMARY KEY (BOARD_ID)
);
```

첨부파일을 연결하는 데 필요한 컬럼이나 테이블, ID 채번용 테이블은 자유롭게 추가해도 됩니다.

## 기능 요구사항

1. 게시판 **목록 · 등록 · 수정 · 삭제** 기능을 제공합니다.
2. 게시글을 등록하거나 수정할 때 **파일을 첨부**할 수 있고(여러 개 가능), 수정 화면에서 첨부 목록을 보고 **내려받을** 수 있습니다.
3. 첨부파일은 파일당 **10MB 까지** 허용합니다. **실행 파일(exe 등)은 업로드를 차단**하고, 차단되거나 용량을 넘으면 서버 오류 화면이 아니라 **안내 화면**을 보여 줍니다.

## 품질 요구사항 (표준프레임워크 방식)

4. **계층 구조**: `Controller` → `Service`(인터페이스) → `ServiceImpl`(`EgovAbstractServiceImpl` 상속, `@Service`) → `Mapper`(`@Mapper`, MyBatis XML) → VO. 패키지는 `egovframework.example.board` 를 권장합니다.
5. **설정은 `@Configuration` 자바 클래스로**: DataSource, MyBatis(SqlSessionFactory), 트랜잭션 관리자, 프로퍼티, 웹(MVC)·보안 설정 등을 XML 이 아닌 자바 설정 클래스로 둡니다.
6. **페이징**(10건 단위, `pageIndex` 파라미터)과 **검색**(`searchCondition`, `searchKeyword` 파라미터, 조건 0=제목 1=내용 2=작성자)을 지원합니다.
7. **트랜잭션**: ServiceImpl 의 메서드가 트랜잭션 관리 대상이어야 합니다 (`TransactionInterceptor` 포인트컷 또는 `@Transactional`).
8. **ID 생성**: 게시글 ID(BOARD_ID)는 사용자가 입력하지 않고 **표준프레임워크 ID Generation 서비스(`EgovIdGnrService`, `EgovTableIdGnrServiceImpl`)** 로 자동 채번합니다.
9. **입력 검증**(제목·작성자 필수, `@Valid` + `BindingResult`)과 **예외 처리**(없는 글을 조회해도 스택트레이스가 아닌 안내 화면)를 적용합니다.
10. **웹 보안**: 사용자 입력은 화면에 이스케이프되어 출력되어야 하고(XSS), CSRF 토큰이 없는 POST 는 거부되어야 합니다.

## 인수 조건 (자동 채점 기준)

아래 URL 과 파라미터 이름을 그대로 지켜야 채점 스크립트가 기능을 확인할 수 있습니다.

| 기능 | 요청 | 파라미터 |
|---|---|---|
| 목록 | `GET /egovBoardList.do` | `pageIndex`, `searchCondition`, `searchKeyword` (모두 선택) |
| 등록 화면 | `POST /addBoardView.do` | |
| 등록 처리 | `POST /addBoard.do` (multipart) | `title`, `content`, `writer`, `file_1`(파일, 여러 개 가능) |
| 수정 화면 | `POST /updateBoardView.do` | `boardId` |
| 수정 처리 | `POST /updateBoard.do` (multipart) | `boardId`, `title`, `content`, `writer`, `atchFileId`, `file_1` |
| 삭제 | `POST /deleteBoard.do` | `boardId` |

- 모든 POST 요청에는 CSRF 토큰(`_csrf`)이 필요합니다. 목록 화면에 토큰이 `<meta name="_csrf" content="...">` 또는 `<input type="hidden" name="_csrf" value="...">` 로 포함되어야 합니다 (채점기가 여기서 토큰을 읽습니다).
- 목록은 `<table>` 로 표시하고, 각 행(`<tr>`)에서 게시글 ID 를 확인할 수 있어야 합니다 (ID 표시, 또는 수정 화면으로 이동하는 `onclick`/링크에 ID 포함).
- 수정 화면에는 `atchFileId` hidden 입력과 첨부 다운로드 `<a href="...">` 링크가 있어야 하고, 다운로드 응답 헤더는 `Content-Disposition: attachment` 여야 합니다.
- 등록 화면에는 사용자가 ID 를 입력하는 칸이 없어야 합니다.
- 제목이 비어 있는 등록 요청은 게시글을 만들지 않고 5xx 오류 없이 처리되어야 합니다.
- 화면에는 KRDS 클래스(`krds-…`)와 KRDS 스타일시트가 사용되어야 합니다.

## 채점 항목

채점기(`npm run verify -- <mcp|nomcp>`)는 아래 18개 항목을 검사합니다. 판정 기준은 저장소의 `docs/milestones.md` 에 있습니다.

| 구분 | 항목 |
|---|---|
| 기본 동작 | B1 빌드 → B2 기동 |
| 기본 기능 | F1 등록 → F2 조회 → F3 수정 → F4 삭제 → F5 업로드 → F6 다운로드 |
| 사용자 편의 | U1 페이징 · U2 검색 · U3 KRDS |
| 시스템 설정 | C1 Config 클래스 · C2 트랜잭션 · C3 ID 생성 · C4 예외/검증 |
| 보안 | S1 첨부파일 보안 · S2 웹 보안(XSS·CSRF·헤더) |
| 표준 준수율 | R1 표준프레임워크 적용률 |
