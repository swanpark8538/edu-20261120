# 과제: 첨부파일을 올릴 수 있는 게시판 (board-app)

이 폴더 안에 **전자정부 표준프레임워크(eGovFrame) 5.0 Boot 기반 게시판 프로젝트**를 새로 만드세요.
프로젝트 폴더 이름은 `board-app` 이어야 합니다 (이 폴더 바로 아래 `board-app/pom.xml` 이 생기도록).

## 1. 업무 요구

게시판 하나를 운영합니다. 사용자는 글을 목록에서 찾아 보고, 새 글을 쓰고, 고치고, 지울 수 있습니다.

1. 글에는 **제목, 내용, 작성자**가 있습니다. 제목과 작성자는 필수입니다. **글 번호는 사용자가 입력하지 않고 시스템이 자동으로 부여**합니다.
2. 목록은 **10건씩 페이지**로 나누어 보여 주고, **제목·내용·작성자 중 하나를 골라 검색**할 수 있습니다.
3. 글을 쓰거나 고칠 때 **파일을 여러 개 첨부**할 수 있고, 수정 화면에서 첨부 목록을 보고 **내려받을** 수 있습니다.
4. 첨부파일은 파일당 **10MB 까지** 허용하고, **실행 파일(exe 등)은 업로드를 차단**합니다.
5. 차단·용량 초과·없는 글 조회 같은 오류 상황에서는 서버 오류 화면이 아니라 **안내 화면**을 보여 줍니다.
6. 화면은 **정부 디자인 시스템(KRDS)** 을 적용합니다.

## 2. 기술 조건

| 항목 | 조건 |
|---|---|
| 프레임워크 | eGovFrame **5.0 Boot** (`org.egovframe.boot:egovframe-boot-starter-parent:5.0.0`), Java 17, Maven. `mvn -DskipTests package` 로 실행 가능한 jar 가 만들어져야 함 |
| 화면 | Thymeleaf + KRDS |
| DB | **내장 HSQL(메모리)**. 별도 DB 설치 없이 기동되어야 하고, 필요한 테이블은 기동 시 자동으로 만들어져야 함 |
| 보안 | 공공 웹 보안 점검 기준 충족 (사용자 입력 이스케이프, CSRF 방어, 보안 응답 헤더 등) |
| 실행 | `java -jar target/*.jar --server.port=<포트> --Globals.fileStorePath=<경로>` 로 기동되어야 함. 첨부파일 저장 경로는 `Globals.fileStorePath` 프로퍼티에서 읽음 |
| 표준 준수 | 표준프레임워크 Boot 의 **표준 프로젝트 구조와 개발 가이드**를 따르고, 실행환경이 제공하는 공통 기반 서비스를 활용할 것. 표준 준수 여부는 별도로 점검함 |

## 3. 테이블

```sql
CREATE TABLE BOARD (
  BOARD_ID VARCHAR(20) NOT NULL,
  TITLE    VARCHAR(200) NOT NULL,
  CONTENT  VARCHAR(4000),
  WRITER   VARCHAR(50) NOT NULL,
  PRIMARY KEY (BOARD_ID)
);
```

첨부파일 연결, 번호 부여 등에 필요한 컬럼이나 테이블은 자유롭게 추가해도 됩니다.

## 4. 화면 · 요청 명세 (인터페이스 계약)

자동 점검기가 아래 URL 과 파라미터 이름을 그대로 호출합니다.

| 기능 | 요청 | 파라미터 |
|---|---|---|
| 목록 | `GET /egovBoardList.do` | `pageIndex`, `searchCondition`(0 제목 · 1 내용 · 2 작성자), `searchKeyword` (모두 선택) |
| 등록 화면 | `POST /addBoardView.do` | |
| 등록 처리 | `POST /addBoard.do` (multipart) | `title`, `content`, `writer`, `file_1`(파일, 여러 개 가능) |
| 수정 화면 | `POST /updateBoardView.do` | `boardId` |
| 수정 처리 | `POST /updateBoard.do` (multipart) | `boardId`, `title`, `content`, `writer`, `atchFileId`, `file_1` |
| 삭제 | `POST /deleteBoard.do` | `boardId` |

- 모든 POST 요청은 CSRF 토큰(`_csrf`)을 요구합니다. 목록 화면에 토큰을 `<meta name="_csrf" content="...">` 또는 `<input type="hidden" name="_csrf" value="...">` 로 포함하세요.
- 목록은 `<table>` 로 표시하고, 각 행(`<tr>`)에서 글 번호를 알 수 있어야 합니다 (번호 표시, 또는 수정 화면으로 가는 링크·`onclick` 에 포함).
- 수정 화면에는 `atchFileId` hidden 입력과 첨부 다운로드 `<a href="...">` 링크가 있어야 하고, 다운로드 응답 헤더는 `Content-Disposition: attachment` 여야 합니다.
