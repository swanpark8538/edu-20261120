# 과제: 첨부파일을 올릴 수 있는 게시판 (board-app)

이 폴더 안에 **전자정부 표준프레임워크(eGovFrame) 5.0 Boot 기반 게시판 프로젝트**를 새로 만드세요.
프로젝트 폴더 이름은 `board-app` 이어야 합니다 (이 폴더 바로 아래 `board-app/pom.xml` 이 생기도록).

## 1. 업무 요구

게시판 하나를 운영합니다. 사용자는 글을 목록에서 찾아 보고, 새 글을 쓰고, 고치고, 지울 수 있습니다.

1. 게시물 CRUD 기능
2. 게시판 목록 화면
3. 게시물 검색 기능
4. 게시물에 첨부파일 기능
5. 화면은 정부 디자인 시스템(KRDS) 적용

## 2. 기술 조건

| 항목 | 조건 |
|---|---|
| 프레임워크 | eGovFrame **5.0 Boot** (`org.egovframe.boot:egovframe-boot-starter-parent:5.0.0`), Java 17, Maven. `mvn -DskipTests package` 로 실행 가능한 jar 가 만들어져야 함 |
| 화면 | Thymeleaf + KRDS |
| DB | **내장 HSQL(메모리)**. 별도 DB 설치 없이 기동되어야 하고, 필요한 테이블은 기동 시 자동으로 만들어져야 함 |
| 보안 | 공공 웹 보안 점검 기준 충족 |
| 실행 | `java -jar <jar 경로> --server.port=<포트> --Globals.fileStorePath=<첨부파일 업로드 폴더 경로>` 로 기동되어야 함. 첨부파일 저장 경로는 `Globals.fileStorePath` 프로퍼티에서 읽음 |
| 표준 준수 | **표준프레임워크 아키텍쳐**를 따르고, 실행환경이 제공하는 공통 기반 서비스를 활용할 것. |

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

점검기는 아래 URL 과 파라미터 이름으로 호출합니다.

| 기능 | 요청 | 파라미터 |
|---|---|---|
| 목록 | `GET /egovBoardList.do` | `pageIndex`(10건 단위), `searchCondition`(0 제목 · 1 내용 · 2 작성자), `searchKeyword` (모두 선택) |
| 등록 화면 | `POST /addBoardView.do` | |
| 등록 처리 | `POST /addBoard.do` (multipart) | `title`, `content`, `writer`, `file_1`(파일, 여러 개 가능) |
| 수정 화면 | `POST /updateBoardView.do` | `boardId` |
| 수정 처리 | `POST /updateBoard.do` (multipart) | `boardId`, `title`, `content`, `writer`, `atchFileId`, `file_1` |
| 삭제 | `POST /deleteBoard.do` | `boardId` |
| 첨부 내려받기 | `GET` (URL 자유) | 첨부 식별자. 응답은 파일 다운로드(`Content-Disposition: attachment`) |

화면 구성은 다음을 따릅니다.

- **등록**: 제목·내용·작성자 입력과 파일 첨부(여러 개).
- **수정**: 기존 내용 수정, 첨부 목록과 첨부별 내려받기 링크, 첨부 추가(기존 첨부 묶음 `atchFileId` 유지), 삭제 버튼.
