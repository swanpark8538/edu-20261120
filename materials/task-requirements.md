# 과제: 첨부파일을 올릴 수 있는 게시판 추가

현재 프로젝트는 전자정부 표준프레임워크(eGovFrame) 5.0 Spring Boot 프로젝트이며, 부서(Dept) 관리 화면이 이미 구현되어 있습니다.
이 프로젝트에 **첨부파일을 올릴 수 있는 게시판(Board)** 을 추가하세요. 기존 Dept 기능이 "이 프로젝트의 표준 구현 예시"입니다.

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

첨부파일을 연결하는 데 필요한 컬럼이나 테이블은 자유롭게 추가해도 됩니다.
앱을 실행하면 내장 DB(HSQL)에 BOARD 테이블과 첨부파일에 필요한 테이블이 자동으로 만들어져야 합니다
(기존 `src/main/resources/db/*.sql` 시드 방식 참고).

## 기능 요구사항

1. 게시판 **목록 · 등록 · 수정 · 삭제** 기능을 제공합니다.
2. 게시글을 등록하거나 수정할 때 **파일을 첨부**할 수 있고(여러 개 가능), 수정 화면에서 첨부 목록을 보고 **내려받을** 수 있습니다.
3. 첨부파일은 파일당 **10MB 까지** 허용합니다. **실행 파일(exe 등)은 업로드를 차단**하고, 차단되거나 용량을 넘으면 서버 오류 화면이 아니라 **안내 화면**을 보여 줍니다.

## 품질 요구사항 (기존 Dept 구현과 같은 수준)

4. 기존 Dept 기능과 같은 **계층 구조**(Controller / Service / ServiceImpl / Mapper / VO)와 **URL 규칙**을 따릅니다. 패키지는 `egovframework.example.board` 를 권장합니다.
5. Board 관련 설정(ID 생성기 빈 등)은 **`@Configuration` 자바 설정 클래스**에 둡니다 (`egovframework.example.config` 의 기존 `EgovConfig*` 클래스 참고).
6. 목록은 **페이징**(10건 단위, `pageIndex` 파라미터)과 **검색**(`searchCondition`, `searchKeyword` 파라미터)을 지원합니다.
7. Board 서비스 구현체(ServiceImpl)의 메서드는 **트랜잭션 관리 대상**이어야 합니다 (`EgovConfigTransaction` 의 포인트컷 범위를 확인하세요).
8. 게시글 ID(BOARD_ID)는 사용자가 입력하지 않고 **표준프레임워크 ID Generation 서비스(EgovIdGnrService)** 로 자동 채번합니다.
9. **입력 검증**(제목·작성자 필수, `@Valid` + `BindingResult`), **예외 처리**(없는 글을 조회해도 스택트레이스가 아닌 안내 화면), **다국어**(화면 문자열은 메시지 번들 `message-common*.properties` 3종에 키를 추가해 `#{...}` 로 표시)를 기존 Dept 와 같은 방식으로 적용합니다.
10. 기존 보안 설정(CSRF 보호 등)은 유지합니다.

## 인수 조건 (자동 채점 기준)

아래 URL 과 파라미터 이름을 그대로 지켜야 채점 스크립트(`npm run verify`)가 기능을 확인할 수 있습니다. 기존 Dept 화면의 URL 규칙과 같습니다.

| 기능 | 요청 | 파라미터 |
|---|---|---|
| 목록 | `GET /egovBoardList.do` | `pageIndex`, `searchCondition`, `searchKeyword` (모두 선택) |
| 등록 화면 | `POST /addBoardView.do` | |
| 등록 처리 | `POST /addBoard.do` (multipart) | `title`, `content`, `writer`, `file_1`(파일, 여러 개 가능) |
| 수정 화면 | `POST /updateBoardView.do` | `boardId` |
| 수정 처리 | `POST /updateBoard.do` (multipart) | `boardId`, `title`, `content`, `writer`, `atchFileId`, `file_1` |
| 삭제 | `POST /deleteBoard.do` | `boardId` |

- 모든 POST 요청에는 CSRF 토큰(`_csrf`)이 필요합니다. 게시판 화면에도 토큰이 포함되어야 합니다 (기존 화면의 `<meta name="_csrf">` / hidden input 참고).
- 목록의 각 행에서 게시글 ID 를 확인할 수 있어야 합니다 (ID 표시, 또는 수정 화면으로 이동하는 `onclick`/링크에 ID 포함).
- 수정 화면의 첨부 다운로드는 `<a href="...">` 링크로 제공하고, 다운로드 응답 헤더는 `Content-Disposition: attachment` 여야 합니다.
- 등록 화면에는 사용자가 ID 를 입력하는 칸이 없어야 합니다.
- 제목이 비어 있는 등록 요청은 게시글을 만들지 않고(검증 실패) 5xx 오류 없이 처리되어야 합니다.
- 다국어 전환은 `?language=en` 파라미터로 동작합니다 (기존 `LocaleChangeInterceptor` 설정).

## 채점 단계

채점기는 아래 순서로 16개 항목을 검사합니다. 자세한 판정 기준은 `docs/milestones.md` 에 있습니다.

| 구분 | 항목 |
|---|---|
| 기본 | B1 빌드 → B2 기동 |
| 기능 | F1 등록 → F2 조회 → F3 수정 → F4 삭제 → F5 업로드 → F6 다운로드 → F7 업로드 보안 |
| 품질 | Q1 Config 클래스 → Q2 페이징 → Q3 검색 → Q4 트랜잭션 → Q5 ID 생성 → Q6 예외/검증/i18n |
| 표준 준수 | S1 표준프레임워크 적용률 (Controller/ServiceImpl/Mapper 계층 검사) |
