# 채점 항목과 판정 기준

`npm run verify -- <mcp|nomcp>` 는 작업 폴더의 `board-app/` 을 임시 디렉터리에 복사해 **빌드 → 기동 → HTTP 요청 → 소스 정적 분석 → 표준 준수 검사** 순으로 19개 항목을 검사합니다.
정의는 `config/milestones.mjs`, 구현은 `scripts/verify.mjs` 에 있습니다. 분류는 교육 문서 "생산성 측정" 의 품질 점검 구분을 따릅니다.

`board-app/pom.xml` 이 없으면 모든 항목을 "프로젝트 없음" 으로 0 처리하고 시각만 기록합니다 (실습 시작 시각 기록용).

## 기본 동작

| ID | 항목 | 판정 |
|---|---|---|
| B1 | Maven 빌드 | `mvn -q -B -DskipTests package` 가 0 으로 끝나고 `target/*.jar` 가 생김 (8분 제한) |
| B2 | 기동 + 목록 화면 | `java -jar … --server.port=<임의> --Globals.fileStorePath=<임시>` 로 기동(150초 제한) 후 `GET /egovBoardList.do` 가 200. `_csrf` 토큰은 목록 화면의 meta/hidden → `/` → 등록 화면(`GET`/`POST /addBoardView.do`) 응답 → `XSRF-TOKEN` 쿠키 순으로 읽음 |

B1 이 실패하면 동적 항목은 모두 "건너뜀"으로 실패 처리됩니다. 정적 항목(C1·C2·C3 일부·C4 일부·U3 일부·R1)은 빌드와 무관하게 검사합니다.

## 기본 기능 (앞 단계가 실패하면 뒤 단계는 건너뜀)

| ID | 항목 | 판정 |
|---|---|---|
| F1 | 등록(C) | `POST /addBoard.do`(multipart: title, content, writer, 빈 file_1) 가 4xx/5xx 가 아니고, 목록에 그 제목이 있으며 **게시글 ID** 를 알 수 있다. 제목이 든 `<tr>` 에서 먼저 찾고(`onclick="…('ID')"` 인자 → `boardId=ID` 링크 → `name="boardId"` hidden 값 → 첫 `<td>` 순), 없으면 등록 전후 화면 전체의 ID 집합(`boardId=…`, hidden `boardId`, 행 패턴) 차이에서 새로 생긴 ID 하나를 고른다 |
| F2 | 조회(R) | `POST /updateBoardView.do`(boardId) 가 200 이고 본문에 등록한 제목이 있다 |
| F3 | 수정(U) | `POST /updateBoard.do`(boardId, 바뀐 title, atchFileId 는 수정 화면의 hidden 값) 후 목록에 바뀐 제목이 있다 |
| F4 | 삭제(D) | `POST /deleteBoard.do`(boardId) 후 목록에서 사라진다 |
| F5 | 업로드 | 2MB PDF 를 `file_1` 로 첨부해 등록 → 수정 화면에 다운로드 링크(`href` 에 `FileDown`, `atchFileId=`, `fileSn=`, `download` 또는 `attach`)가 있다 |
| F6 | 다운로드 | 링크 중 하나를 GET 하면 200, **내용이 업로드한 바이트와 같고**, `Content-Disposition` 에 `attachment` |

## 사용자 편의

| ID | 항목 | 판정 |
|---|---|---|
| U1 | 페이징 | 글 12건 등록 후 목록 1페이지에 12건이 모두 나오지 않고, `?pageIndex=2` 와 합치면 모두 나오며, 화면에 `pageIndex`/`pagination`/`paging` 문자열이 있다 |
| U2 | 검색 | 12건 중 하나의 제목에 든 고유 토큰으로 `?searchKeyword=토큰`(searchCondition 0 → 1 → 2 → 없음 순) 요청 시 그 글만 나온다. 없는 검색어로 요청해도 오류 화면이 아니다 |
| U3 | KRDS | 목록 HTML 에 `krds-` 클래스가 3개 이상, KRDS 스타일시트(`krds*.css` 또는 `egovframe.css`) 링크가 있고, 리소스에 이름에 `krds` 가 든 파일이 있다 |

## 시스템 설정

| ID | 항목 | 판정 |
|---|---|---|
| C1 | Config 클래스 | `@Configuration` 클래스가 3개 이상이고, 그 소스들에 `DataSource`, `SqlSessionFactory`, `TransactionManager` 가 모두 등장한다 (정적) |
| C2 | 트랜잭션 | `*Board*ServiceImpl.java` 에 `@Transactional` 이 있거나, `TransactionInterceptor`/`txAdvice` 를 정의한 클래스의 `execution(...)` 포인트컷(`..`, `*` 해석)이 `패키지.클래스.insertBoard` 와 일치한다 (정적) |
| C3 | ID 생성 | (a) Board 소스에서 `IdGnrService`/`getNextStringId` 사용 (정적), (b) 등록 화면에 hidden/readonly 가 아닌 `name="boardId"` 입력란이 없다, (c) F1 에서 서버가 ID 를 채번했다 |
| C4 | 예외/검증 | (정적) Board 소스에 검증 어노테이션(`@Valid`/`@NotBlank`/`@EgovNullCheck` 등). (검증) 빈 제목 등록이 5xx 가 아니고 목록의 ID 집합이 늘지 않는다. (예외) 없는 boardId 로 수정 화면 요청 시 본문에 `Whitelabel Error Page` 나 Java 스택트레이스가 없다 (안내 화면이면 500 이어도 통과) |

## 패키지 구성

| ID | 항목 | 판정 |
|---|---|---|
| P1 | 도메인별 패키지 분리 | (정적) 루트 = `@SpringBootApplication` 클래스의 패키지. ① Board 계층 클래스(`*Board*Controller/Service/ServiceImpl/Mapper/DAO`)의 공통 상위 패키지가 루트보다 깊어야 한다 (예: `루트.board`). 계층 패키지가 루트 바로 아래(`루트.web`, `루트.service`)면 도메인 패키지가 없는 것으로 본다. ② 첨부 기능 클래스(이름에 `File`/`Atch`/`Attach`/`Upload`/`Download`/`Storage`, `Board*`·`*Config*` 제외)는 Board 도메인 패키지 밖(예: `cmm`, `cmmn.fms`, 별도 도메인 패키지)에 있어야 하고, Board 계층 클래스 안에 파일 저장·다운로드 처리(`transferTo`, `Files.copy`, `Content-Disposition` 등)가 직접 들어 있으면 안 된다. ③ `@Configuration` 클래스는 Board 도메인 패키지 밖(예: `config`)에 있어야 한다. ④ 한 패키지에 `@Controller` 와 `@Service`/`@Repository`/`@Mapper` 구현이 함께 있으면 안 된다. 넷 모두 만족 |

P1 은 파일 이름과 패키지 경로를 보는 휴리스틱이므로 비정형 구조에서는 오판할 수 있습니다. 센터의 AI 점검에서 같은 기준으로 다시 확인합니다.

## 보안

| ID | 항목 | 판정 |
|---|---|---|
| S1 | 첨부파일 보안 | (a) `evil.exe` 첨부 등록이 5xx 가 아니고, 글이 생겼더라도 그 첨부를 내려받았을 때 exe 내용이 아니다. (b) 11MB PDF 첨부 등록이 5xx 가 아니고 게시글이 생기지 않는다. F5 통과 전제 |
| S2 | 웹 보안 | (XSS) `<svg/onload=alert(1)>` 가 든 제목을 등록했을 때 목록·수정 화면에 원문 태그가 그대로 나오지 않는다. (CSRF) `_csrf` 없는 등록 POST 로 게시글이 생기지 않는다. (헤더) 목록 응답에 `X-Content-Type-Options: nosniff` 와 `X-Frame-Options`(또는 CSP `frame-ancestors`) 가 있다. 셋 모두 만족 |

## 표준 준수율

| ID | 항목 | 판정 |
|---|---|---|
| R1 | 표준프레임워크 적용률 | `egovframe-mcp-check` 의 `egov_check_compliance` 를 채점기가 stdio 로 직접 호출(AI 에 연결하지 않음). **위반 0건**, `*Board*(Controller|ServiceImpl|DAO|Mapper).java` 가 **3개 이상**. 적용률(%)은 결과에 함께 기록 |

검사 규칙(check MCP): Controller 는 `@Controller`/`@RestController` + 매핑 어노테이션, ServiceImpl 은 `EgovAbstractServiceImpl` 상속 + `@Service` + `implements …Service`, DAO 는 `@Repository` + `EgovAbstractDAO`/`EgovAbstractMapper` 상속, Mapper 는 `@Mapper`/`@EgovMapper`.

## 결과 기록

- 실행마다 `submissions/<ID>/<run>/progress.jsonl` 에 한 줄(JSON): 점수, 구분별 점수, 적용률, 항목별 통과 여부와 사유, 시작·종료 시각, 프로젝트 존재 여부.
- `npm run collect` 는 최종 결과를 `submissions/<ID>/<run>/final.json` 에 저장하고 산출물 사본을 `project/` 에 둡니다.
- 진행자는 `npm run aggregate` 로 참가자별 두 실행을 짝지은 보고서 `submissions/report.md` 를 만듭니다.

## 참조 프로젝트의 기대 점수

| 대상 | 기대 결과 |
|---|---|
| 빈 작업 폴더 | 0/19 (프로젝트 없음) |
| `npm run reference` (scaffold Board → 첨부 인프라 설치, 손 수정 없음) | 첨부가 Board 에 연결되지 않아 F5·F6·S1 실패, 나머지 통과 → **16/19** |
| 첨부 연결까지 마친 완성 프로젝트 | 19/19 |
