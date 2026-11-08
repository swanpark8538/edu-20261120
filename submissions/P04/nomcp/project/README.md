# 열린 게시판 · board-app

TASK.md의 요청 계약을 구현한 eGovFrame 5.0 Boot / Java 17 / Maven 게시판입니다.

## 빌드 및 실행

프로젝트 폴더에서 다음 명령을 실행합니다.

```powershell
mvn -DskipTests package
java -jar target/board-app-1.0.0.jar --server.port=8080 --Globals.fileStorePath=C:/board-uploads
```

접속: <http://localhost:8080/egovBoardList.do>

경로에 공백이 있으면 `"--Globals.fileStorePath=C:/my uploads"`처럼 인자 전체를 따옴표로 감쌉니다. 기본 업로드 경로는 실행 디렉터리의 `./uploads`이며, 폴더는 자동 생성됩니다. 실행 계정에 해당 경로의 읽기·쓰기 권한이 필요합니다.

내장 HSQL 메모리 DB를 사용합니다. 외부 DB 설치 없이 `schema.sql`로 테이블과 번호 관리 초기값을 만듭니다. **프로세스를 재시작하면 게시물과 첨부 메타데이터는 초기화됩니다.** 디스크 첨부파일은 재시작 시 자동 삭제하지 않으며 이전 첨부파일을 다시 조회할 수는 없습니다. 메모리 DB 실습 환경에서는 실행별 업로드 폴더를 사용하는 것이 편리합니다.

## 기능과 요청 계약

| 기능 | 요청 | 파라미터 |
|---|---|---|
| 목록·검색 | GET `/egovBoardList.do` | `pageIndex`(1부터, 페이지당 10개), `searchCondition`(0 제목, 1 내용, 2 작성자), `searchKeyword` |
| 등록 화면 | POST `/addBoardView.do` | 없음 |
| 등록 | POST `/addBoard.do` multipart | `title`, `content`, `writer`, `file_1`(동일 이름으로 복수 파일) |
| 내용 확인·수정 화면 | POST `/updateBoardView.do` | `boardId` |
| 수정 | POST `/updateBoard.do` multipart | `boardId`, `title`, `content`, `writer`, `atchFileId`, `file_1` |
| 삭제 | POST `/deleteBoard.do` | `boardId` |
| 다운로드 | GET `/downloadFile.do` | `fileId` |

목록의 제목을 누르면 내용 확인·수정 화면으로 이동합니다. 수정 시 기존 첨부와 `atchFileId`는 유지되고 새로운 파일이 추가됩니다. 삭제 영역을 펼친 뒤 삭제하면 게시물과 모든 첨부를 함께 삭제합니다.

제목 200자, 내용 4,000자, 작성자 50자까지 입력합니다. 제목·작성자는 필수입니다. 첨부는 게시물당 10개, 파일당 10MB, 요청당 55MB 이내입니다. 허용 확장자는 TXT, CSV, PDF, PNG, JPG, JPEG, GIF, ZIP, DOC, DOCX, XLS, XLSX, PPT, PPTX, HWP, HWPX입니다.

## 구조와 공통 서비스

```text
src/main/java/egovframework/board/
  config/             MyBatis, eGov 공통 서비스, 보안, multipart 제한
  web/                Controller, 예외 응답
  service/            Service 인터페이스, 입력·조회 VO
  service/impl/       트랜잭션 업무 서비스, DAO, 디스크 파일 저장
src/main/resources/
  mapper/             MyBatis 파라미터 바인딩 SQL
  templates/          Thymeleaf 화면
  static/vendor/krds/  공식 KRDS CSS와 로컬 아이콘
  schema.sql          HSQL 테이블 자동 생성
```

- 부모 POM: `org.egovframe.boot:egovframe-boot-starter-parent:5.0.0`
- 업무 계층: `EgovAbstractServiceImpl` + `LeaveaTrace` 공통 기반 서비스
- 데이터 계층: `EgovAbstractMapper` + MyBatis + Spring 트랜잭션
- 번호 부여: `EgovTableIdGnrServiceImpl`과 IDS 테이블. 게시물 ID는 20자입니다.
- 페이징: `PaginationInfo`, 10건 단위 목록과 10페이지 단위 페이지 링크
- 화면: Thymeleaf + KRDS 공식 컴포넌트 CSS를 로컬 제공. 별도 Node 빌드나 CDN 연결이 필요하지 않습니다.

## 보안 구현 범위

요구사항에 사용자 계정이나 소유권 구분이 없어 누구나 게시물을 작성·수정·삭제하는 공개 게시판으로 구현했습니다.

- 모든 POST에 Spring Security CSRF 검증을 적용합니다. 브라우저 양식에는 토큰이 자동 포함됩니다. 자동 호출 시 먼저 목록을 GET하여 세션 쿠키와 숨겨진 `_csrf` 값을 얻고, 같은 세션의 POST에 토큰을 전달해야 합니다. 보안 검증을 끄는 예외 URL은 없습니다.
- multipart 데이터는 서블릿 용량 제한을 적용한 뒤 CSRF 토큰을 확인합니다. 초과 업로드는 HTTP 413으로 거부합니다.
- MyBatis `#{...}` 바인딩으로 SQL 주입을 방지하고, 검색의 `%`, `_`는 문자 그대로 처리합니다. 검색 컬럼은 정해진 세 값으로만 선택합니다.
- Thymeleaf 이스케이프 출력, CSP, `nosniff`, 프레임 삽입 차단, HttpOnly·SameSite 세션 쿠키를 적용합니다.
- 서버에서 길이·필수값·검색값을 검사하고 바인딩 허용 필드를 제한합니다. 오류 화면에는 스택 트레이스나 DB 오류를 표시하지 않습니다.
- 파일명 경로 구분자와 제어문자를 거부하고 확장자·크기·개수를 검사합니다. 실제 저장명은 UUID이며 업로드 경로를 정적 리소스로 공개하지 않습니다. 파일 ID로 DB를 조회한 뒤 검증된 저장 경로에서 다운로드합니다.
- 다운로드는 원래 한글 파일명을 보존하며 `application/octet-stream`과 `Content-Disposition: attachment`를 사용합니다.
- 다른 게시물의 첨부 묶음을 제출하면 거부합니다. 수정·삭제는 행 잠금으로 직렬화하고, 저장 실패 시 DB 롤백과 새 파일 정리를 수행합니다. 삭제 파일은 DB 커밋 후 정리합니다.

운영기관의 별도 공공 웹 보안 인증이나 전 항목 진단을 수행했다는 의미는 아닙니다. 운영 배포의 계정·권한 정책과 TLS는 기관 환경에서 정해야 합니다.

## 검증

```powershell
mvn test
```

`BoardIntegrationTest`는 실제 HSQL DB와 Spring Security 필터를 사용해 CRUD, 복수 첨부, 다운로드, 추가 수정, 삭제, 검색 3종, 페이징, HTML 이스케이프, SQL 주입 입력, CSRF, 입력 길이, 파일명 경로, 허용 확장자, 첨부 묶음 변조, 파일 개수 제한을 확인합니다. 테스트용 파일은 별도 임시 폴더에 저장하고 정리합니다.

실행 JAR의 HTTP 흐름은 PowerShell 7에서 다음처럼 확인할 수 있습니다. 서버를 먼저 18084 포트로 실행해야 합니다.

```powershell
./scripts/smoke.ps1 -BaseUrl http://localhost:18084
```

실제 세션 쿠키와 화면의 토큰으로 multipart 등록·다운로드·추가 수정·삭제·CSRF 거부를 검증합니다.

## 참고 자료

- [eGovFrame 공식 Boot 샘플](https://github.com/eGovFramework/egovframe-boot-sample-java-config)
- [eGovFrame 5.0 Boot 의존성 가이드](https://www.egovframe.go.kr/docs/5.0/egovframe-development/deployment-tool/build-tool/gradle/boot-to-gradle/)
- [KRDS 공식 개발자 시작 안내](https://www.krds.go.kr/html/site/outline/outline_03.html)
- 외부 자산 출처는 [THIRD_PARTY.md](THIRD_PARTY.md)에 기록했습니다.
