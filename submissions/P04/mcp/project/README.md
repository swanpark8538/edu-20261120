# board-app

eGovFrame 5.0 Boot / Java 17 / Maven / Thymeleaf / KRDS 게시판.

## 빌드 및 실행

```powershell
mvn -DskipTests package
java -jar target/board-app-1.0.0.jar --server.port=8080 --Globals.fileStorePath=C:/data/board-uploads
```

접속: http://localhost:8080/egovBoardList.do

내장 HSQL 메모리 DB를 사용하며 기동 시 게시물·첨부·채번 테이블이 자동 생성됩니다. 재기동하면 DB 데이터는 초기화됩니다. 파일 저장 폴더는 웹 정적 리소스 밖의 경로를 사용하세요.

- TASK.md의 URL 및 파라미터 계약 적용, 목록 10건 단위 페이징, 제목/내용/작성자 검색.
- 등록/수정에서 file_1 다중 첨부, 수정 시 기존 atchFileId 유지 및 첨부별 다운로드.
- 표준 EgovFileMngService/EgovFileMngUtil 및 별도 FILE_ID 채번 서비스 사용.
- CSRF 기본 활성: POST 요청은 세션 쿠키와 화면의 _csrf hidden 토큰을 함께 전송해야 합니다.
- 파일당 10MB, 요청당 50MB. 실행 가능한 확장자 차단, 경로 검증, HTML 이스케이프, 바인딩 SQL 및 서버 입력 길이 검증 적용.
- 삭제 시 공통 첨부 서비스의 표준 논리 삭제(USE_AT=N)를 호출합니다.

## 검증

mvn -DskipTests package 성공. 실행 중인 서버(18084 포트)에 scripts/smoke.py를 실행해 CRUD, 검색 세 조건, 다중 첨부, 다운로드, 첨부 추가/그룹 유지, CSRF 없는 POST 차단을 확인했습니다.

```powershell
python scripts/smoke.py
```

공개 게시판으로 인증 없이 접근합니다. 공식 보안 점검 인증을 수행한 결과는 아닙니다.
