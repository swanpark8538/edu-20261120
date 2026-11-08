# board-app

egovframe-codegen MCP로 생성한 eGovFrame **5.0 Boot / Java 17 / Maven** 게시판입니다. Thymeleaf + 번들 KRDS 리소스를 사용하며, 외부 DB나 CDN 없이 실행됩니다.

## 빌드 및 실행

```sh
cd board-app
mvn -DskipTests package
java -jar target/board-app-1.0.0.jar --server.port=8080 --Globals.fileStorePath=/tmp/board-app-uploads
```

접속: http://localhost:8080/egovBoardList.do

`Globals.fileStorePath`로 지정한 폴더를 자동 생성하며, 해당 경로에 쓰기 권한이 필요합니다. 미지정 기본값은 `${java.io.tmpdir}/board-app-uploads`입니다. 정적 리소스 디렉터리는 업로드 경로로 사용할 수 없습니다.

HSQL 메모리 DB에 게시판·첨부파일·채번 테이블을 기동 시 자동 생성합니다. **앱 재시작 시 게시물 및 파일 메타데이터는 초기화됩니다.** 디스크에 저장된 파일은 재시작으로 삭제되지 않습니다.

## 기능 및 요청 계약

| 기능 | 요청 | 파라미터 |
|---|---|---|
| 목록 / 검색 | GET `/egovBoardList.do` | `pageIndex`(페이지당 10건), `searchCondition`(0 제목 / 1 내용 / 2 작성자), `searchKeyword` |
| 등록 화면 | POST `/addBoardView.do` | CSRF 토큰 |
| 등록 | POST `/addBoard.do` (multipart) | `title`, `content`, `writer`, `file_1`(동일 이름으로 복수 파일) |
| 수정 화면 | POST `/updateBoardView.do` | `boardId` |
| 수정 | POST `/updateBoard.do` (multipart) | `boardId`, `title`, `content`, `writer`, `atchFileId`, `file_1` |
| 삭제 | POST `/deleteBoard.do` | `boardId` |
| 다운로드 | GET `/cmm/fms/FileDown.do` | `atchFileId`, `fileSn` |

모든 POST 요청은 CSRF 토큰이 필요합니다. 목록 페이지에서 세션 쿠키와 `_csrf` meta/hidden 값을 얻은 뒤 `_csrf` 폼 파라미터 또는 `X-CSRF-TOKEN` 헤더로 전송합니다. 브라우저 화면은 이를 자동 처리합니다.

제목(200자)과 작성자(50자)는 필수이며, 내용은 선택 입력(최대 4000자)입니다. 첨부는 파일당 10MB, 요청당 50MB까지 허용합니다. 수정 화면에서 기존 첨부 다운로드와 파일 추가, 게시물 전체 삭제가 가능합니다.

## 구조 및 보안

- Controller → Service(`EgovAbstractServiceImpl`) → MyBatis `@EgovMapper` 구조, eGovFrame 프로퍼티·트랜잭션·예외처리·페이징·테이블 ID 생성 서비스를 사용합니다.
- `BOARD.ATCH_FILE_ID`로 표준 `COMTNFILE` / `COMTNFILEDETAIL` 그룹에 연결합니다. 파일 ID는 `egovFileIdGnrService`가 생성합니다.
- 수정·삭제 시 첨부 그룹은 DB의 게시물에서 조회하므로 요청의 `atchFileId`를 다른 그룹으로 바꿔도 적용되지 않습니다. 행 잠금으로 동시 첨부 추가를 직렬화합니다.
- 게시물과 파일 메타데이터는 동일 트랜잭션에서 반영합니다. 파일 처리 실패나 DB 롤백 시 새 파일을 정리합니다.
- 게시물 삭제 시 표준 서비스로 첨부 그룹을 비활성화하고, 커밋 후 저장 파일을 삭제합니다. 비활성 그룹은 다운로드할 수 없습니다.
- CSRF 기본 활성, StrictHttpFirewall, 보안 응답 헤더, 바인딩 필드 제한, 서버 입력 검증, MyBatis 파라미터 바인딩, Thymeleaf 출력 이스케이프를 적용했습니다.
- 실행 가능한 확장자를 차단하며, 저장 파일명은 무작위로 재생성합니다. 다운로드는 경로를 요청받지 않고 DB 파일 식별자로 조회하며 `Content-Disposition: attachment`로 반환합니다.
- 로그인 요구사항이 없는 공개 게시판으로, 모든 사용자가 게시물 CRUD를 사용할 수 있습니다. 인증·작성자별 권한 기능은 포함하지 않습니다.

## 검증

서버를 실행한 상태에서 Python 3 표준 라이브러리만으로 확인할 수 있습니다. 별도 테스트 데이터로 등록·수정·검색·페이징·첨부 다운로드·삭제 및 보안 동작을 검증하고 생성한 게시물을 삭제합니다.

```sh
python3 scripts/smoke_test.py http://localhost:8080
```

빌드와 HTTP 검증 결과는 `docs/verification.md`에 기록했습니다. 공공 웹 보안 인증이나 외부 취약점 진단을 대체하는 결과는 아닙니다.
