# 검증 결과

검증일: 2026-11-08 (Asia/Seoul)

## 생성

`egovframe-codegen.egov_scaffold_project`로 eGovFrame 5.0 Boot 게시판을 생성하고, `egov_add_attach_file_infrastructure`로 표준 첨부파일 기반 서비스를 설치했습니다. 게시판 VO·DDL·매퍼·서비스·화면에 첨부 연결을 추가했습니다.

## 빌드 및 기동

- Java: Eclipse Adoptium 17.0.18
- Maven: 3.9.9
- `mvn -DskipTests package`: **BUILD SUCCESS**
- 결과: `target/board-app-1.0.0.jar` (Spring Boot 실행 JAR)
- `java -jar target/board-app-1.0.0.jar --server.port=18081 --Globals.fileStorePath=/tmp/board-app-final-verification`: 정상 기동
- 기본 hsql 프로파일로 별도 DB 없이 테이블 자동 초기화
- CSRF 활성 상태에서 HTTP 검증 수행

## 기능 검증

Python 3 표준 라이브러리로 작성한 `scripts/smoke_test.py`의 실제 실행 결과입니다. 테스트 후 게시물을 정리했으며 지정 업로드 폴더가 빈 상태임을 확인했습니다. 브라우저에서 KRDS CSS가 적용된 등록 화면과 다중 파일 입력도 확인했습니다.

```text
PASS: 내장 HSQL 목록 화면
PASS: 보안 응답 헤더
PASS: CSRF 없는 POST 거부
PASS: 등록 화면
PASS: 게시물과 첨부파일 2개 등록
PASS: 게시물 XSS 출력 이스케이프
PASS: 수정 화면 조회
PASS: 수정 화면 첨부 목록 2개
PASS: 첨부 다운로드 헤더 및 원본 바이트
PASS: 첨부 다운로드 헤더 및 원본 바이트
PASS: 수정 및 첨부 추가
PASS: 수정 화면 조회
PASS: 요청의 첨부 ID 변조 무시 및 기존 그룹 유지
PASS: 수정 화면 조회
PASS: 동시 첨부 추가의 연번 충돌 방지
PASS: 검색 조건 0
PASS: 검색 조건 1
PASS: 검색 조건 2
PASS: 검색 SQL 주입 문자열 처리
PASS: 서버 필수값 검증
PASS: 서버 길이 검증
PASS: 수정 화면 조회
PASS: 실행 파일 차단과 복수 업로드 롤백
PASS: 게시물 삭제
PASS: 삭제한 게시물의 첨부 다운로드 차단
PASS: 삭제 목록 반영
PASS: 내용 선택 입력 등록 0
PASS: 내용 선택 입력 등록 1
PASS: 내용 선택 입력 등록 2
PASS: 내용 선택 입력 등록 3
PASS: 내용 선택 입력 등록 4
PASS: 내용 선택 입력 등록 5
PASS: 내용 선택 입력 등록 6
PASS: 내용 선택 입력 등록 7
PASS: 내용 선택 입력 등록 8
PASS: 내용 선택 입력 등록 9
PASS: 내용 선택 입력 등록 10
PASS: 10건 단위 페이징 1
PASS: 10건 단위 페이징 2
PASS: 검증 데이터 정리
PASS: 검증 데이터 정리
PASS: 검증 데이터 정리
PASS: 검증 데이터 정리
PASS: 검증 데이터 정리
PASS: 검증 데이터 정리
PASS: 검증 데이터 정리
PASS: 검증 데이터 정리
PASS: 검증 데이터 정리
PASS: 검증 데이터 정리
PASS: 검증 데이터 정리
모든 HTTP 검증 통과
```

## 검증 범위

이 결과는 명시된 기능 및 HTTP 보안 동작의 로컬 실행 검증입니다. 외부 보안 진단이나 공공기관 보안 적합성 인증은 수행하지 않았습니다. 테스트 서버는 검증 후 종료했습니다.
