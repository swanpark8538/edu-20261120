# board-app

TASK.md의 요청 명세에 맞춘 첨부파일 게시판입니다. Java 17, Maven, eGovFrame Boot **5.0.0**, Thymeleaf, 메모리 HSQLDB, KRDS로 구성합니다. MCP 도구나 생성기는 사용하지 않았습니다.

## 빌드 및 실행

```sh
cd board-app
mvn -DskipTests package
java -jar target/board-app-1.0.0.jar --server.port=8080 --Globals.fileStorePath=/absolute/path/to/board-uploads
```

브라우저에서 http://localhost:8080/egovBoardList.do 로 접속합니다. `/`도 목록으로 이동합니다. DB 설치나 별도 환경 변수 설정은 필요 없습니다. 첨부파일 저장 폴더는 없으면 자동 생성하며 해당 경로에 쓰기 권한이 필요합니다. 기본 경로는 실행 디렉터리의 `./uploads`입니다.

**메모리 DB는 종료하면 게시물과 파일 메타데이터가 초기화됩니다.** 실제 첨부파일은 디스크에 저장하므로 재기동 시 이전 파일은 연결되지 않습니다. 요구사항에 맞춘 개발용 구성으로, 영속 운영 시에는 DB 및 파일의 보관 정책을 함께 변경해야 합니다.

## 사용 방법

- 목록에서 제목을 누르면 상세 내용과 첨부파일을 확인합니다.
- 글쓰기에서 제목, 작성자, 내용을 입력하고 파일을 여러 개 선택합니다.
- 상세 화면의 수정 버튼으로 기존 첨부파일을 유지하면서 내용을 고치고 파일을 추가합니다.
- 상세 또는 수정 화면의 삭제 버튼으로 게시물과 모든 첨부파일을 삭제합니다.
- 제목 / 내용 / 작성자로 검색하며, 페이지당 10건을 표시합니다.

## 인터페이스

| 메서드 | URL | 주요 입력 |
|---|---|---|
| GET | `/egovBoardList.do` | `pageIndex`, `searchCondition`(0/1/2), `searchKeyword` |
| POST | `/addBoardView.do` | 없음 |
| POST, multipart | `/addBoard.do` | `title`, `content`, `writer`, 반복되는 `file_1` |
| POST | `/updateBoardView.do` | `boardId` |
| POST, multipart | `/updateBoard.do` | `boardId`, `title`, `content`, `writer`, `atchFileId`, 반복되는 `file_1` |
| POST | `/deleteBoard.do` | `boardId` |
| GET | `/boardDetail.do` | `boardId` |
| GET | `/downloadFile.do` | `fileId` |

모든 POST 요청에는 CSRF 토큰이 필요합니다. 브라우저에서는 Thymeleaf가 자동 삽입합니다. 외부 점검 클라이언트는 먼저 목록을 GET해 세션 쿠키와 `<input name="_csrf">`의 값을 취득하고, 같은 세션에서 `_csrf` 파라미터 또는 `X-CSRF-TOKEN` 헤더로 전송해야 합니다. multipart 요청도 동일합니다. CSRF 검사를 끄는 예외 경로는 없습니다.

등록 성공은 목록으로 리다이렉트하며 등록한 게시물 ID는 목록의 상세 링크에서 확인할 수 있습니다. 첨부 묶음 ID와 파일 ID는 수정/상세 화면에서 확인합니다. 수정 시 `atchFileId`를 전송하면 실제 게시물의 묶음과 일치해야 합니다. 생략해도 서버가 기존 묶음을 유지하며, 클라이언트 입력으로 다른 게시물의 파일을 연결하지 않습니다.

## 구조 및 표준 실행환경 사용

```text
src/main/java/egovframework/board/
  config/          DB / MyBatis / eGov ID 생성 / trace / 메시지 / 보안 구성
  web/             Controller, 오류 응답
  service/         Service 인터페이스, 게시물·검색·첨부 모델
  service/impl/    Service 구현, DAO, 파일 저장소
src/main/resources/
  mapper/          파라미터 바인딩 기반 MyBatis SQL
  templates/       Thymeleaf 화면
  static/          로컬 KRDS CSS·아이콘·폰트, 화면 스타일과 스크립트
  schema.sql       BOARD / BOARD_FILE / IDS 자동 생성
```

- Service는 `EgovAbstractServiceImpl`을 상속하고 `BoardService` 인터페이스를 구현합니다.
- DAO는 `EgovAbstractMapper`를 상속합니다.
- 게시물 번호는 `EgovTableIdGnrServiceImpl`과 IDS 테이블로 발급합니다.
- 공통 trace(`LeaveaTrace`)와 실행환경 메시지를 구성합니다.
- 페이지 계산은 `PaginationInfo`를 사용합니다.
- 게시물·첨부 메타데이터는 트랜잭션으로 처리합니다. 롤백 시 신규 물리 파일을 정리하고, 삭제 커밋 후 기존 물리 파일을 정리합니다.

## 보안 및 업로드 정책

이 게시판은 업무 요구에 따라 로그인 없이 공동으로 편집하는 공개 게시판입니다. 작성자 이름은 인증 정보가 아니며 모든 방문자가 게시물을 수정·삭제할 수 있습니다. 소유자별 권한 분리가 필요한 운영 환경은 인증과 인가 정책을 추가해야 합니다.

- CSRF 기본 보호 유지, POST만으로 등록·수정·삭제.
- Thymeleaf 텍스트 이스케이프로 저장형 XSS 방어, 원문 HTML 실행 없음.
- SQL의 동적 열 이름은 고정 분기만 사용하며 입력은 전부 `#{...}` 바인딩. 검색의 `%`, `_`는 리터럴로 처리.
- 제목 200자, 작성자 50자, 본문 4,000자 등 서버 검증.
- 파일 경로·제어문자가 포함된 원본명 거부, 서버 생성 UUID 이름으로 웹 루트 외부에 저장.
- 다운로드는 DB에서 조회한 파일만 허용하고 경로·심볼릭 링크를 검증. 원본 파일명은 UTF-8로 인코딩하며 `attachment`, `application/octet-stream`, `nosniff` 사용.
- 파일은 한 번에 최대 10개, 개당 10MB, 합계 45MB. HTTP multipart 요청 한도는 50MB.
- 허용 확장자: txt, csv, pdf, png, jpg, jpeg, gif, webp, zip, hwp, hwpx, doc, docx, xls, xlsx, ppt, pptx, odt. 실행 파일·HTML·SVG 등은 거부. 별도 악성코드 검사 엔진은 포함하지 않습니다.
- CSP, 프레임 차단, Referrer-Policy, HttpOnly / SameSite 세션 쿠키 사용.
- 오류 응답에 내부 예외나 스택을 노출하지 않으며 서버 로그에 기록.

HTTPS 배포 시 `--server.servlet.session.cookie.secure=true`를 적용하세요. 공식 보안 인증 또는 외부 취약점 진단을 받은 결과물은 아닙니다.

## 검증

```sh
mvn test
mvn -DskipTests package
```

통합 테스트에서 CRUD, 다중 업로드, 파일 내려받기, 첨부 묶음 유지, 물리 파일 삭제, 검색 3종, 10건 페이징, CSRF, XSS 이스케이프, SQL 입력, 파일명·확장자 거부, 입력 길이와 묶음 변조, 범위를 넘는 페이지 요청, 업로드 실패 시 DB·파일 롤백을 검증합니다. 테스트는 임시 업로드 디렉터리를 사용합니다. 샌드박스에서도 테스트가 동작하도록 Mockito는 subclass mock maker를 사용합니다.

## KRDS

공식 `krds-uiux@1.0.3`의 CSS, 아이콘, Pretendard GOV 폰트를 로컬 제공하며 반응형 화면과 키보드 포커스, 본문 바로가기, 필드 레이블·오류 메시지를 적용했습니다. 화면 렌더링에 외부 CDN이 필요하지 않습니다. 출처와 배포 정보는 `THIRD_PARTY_NOTICES.md`를 참고하세요.

검증 결과: 통합 테스트 **5개 통과**, `mvn -DskipTests package` 성공. 실행 JAR를 `--server.port=18080 --Globals.fileStorePath=/tmp/board-app-smoke-uploads`로 기동한 실제 Chromium에서도 등록(첨부 2개) → 상세 → 수정 및 첨부 추가 → 다운로드 바이트 확인 → 첨부 묶음 유지 → 삭제를 확인했습니다. 데스크톱(1440px)과 모바일(390px)의 화면 및 가로 넘침 여부도 확인했습니다.
