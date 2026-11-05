# 채점 항목과 판정 기준

`npm run verify` 는 `board-app/` 을 임시 디렉터리에 복사해 **빌드 → 기동 → HTTP 요청 → 소스 정적 분석 → 표준 준수 검사** 순으로 16개 항목을 검사합니다.
정의는 `config/milestones.mjs`, 구현은 `scripts/verify.mjs` 에 있습니다.

## 기본

| ID | 항목 | 판정 |
|---|---|---|
| B1 | 빌드 성공 | `mvn -q -B -DskipTests package` 가 0 으로 끝나고 `target/*.jar` 가 생김 (8분 제한) |
| B2 | 기동 + 목록 화면 | `java -jar` 로 임의 포트에 기동(150초 제한) 후 `GET /egovBoardList.do` 가 200. 이 화면에서 `_csrf` 토큰을 읽는다 (없으면 Dept 화면에서 빌리고 경고) |

B1 이 실패하면 B2 이하 동적 항목은 모두 "건너뜀"으로 실패 처리됩니다. 정적 항목(Q1·Q4·Q5 일부·Q6 일부·S1)은 빌드와 무관하게 검사합니다.

## 기능

앞 단계가 실패하면 뒤 단계는 건너뜁니다 (C → R → U → D, 업로드 → 다운로드 → 보안).

| ID | 항목 | 판정 |
|---|---|---|
| F1 | 등록(C) | `POST /addBoard.do`(multipart: title, content, writer, 빈 file_1) 가 4xx/5xx 가 아니고, `GET /egovBoardList.do` 의 어떤 행에 그 제목이 있으며 그 행에서 **게시글 ID** 를 뽑을 수 있다. ID 는 `onclick="…('ID')"` 인자 → `boardId=ID` 링크 → 첫 번째 `<td>` 순으로 찾는다 |
| F2 | 조회(R) | `POST /updateBoardView.do`(boardId) 가 200 이고 본문에 등록한 제목이 있다 |
| F3 | 수정(U) | `POST /updateBoard.do`(boardId, 바뀐 title, atchFileId 는 수정 화면의 hidden 값) 후 목록에 바뀐 제목이 있다 |
| F4 | 삭제(D) | `POST /deleteBoard.do`(boardId) 후 목록에서 사라진다 |
| F5 | 업로드 | 2MB PDF 를 `file_1` 로 첨부해 등록 → 목록에서 ID 확보 → 수정 화면에 다운로드 링크(`href` 에 `FileDown`, `atchFileId=` 또는 `download` 포함)가 있다 |
| F6 | 다운로드 | 링크 중 하나를 GET 하면 200 이고 **내용이 업로드한 바이트와 같으며** `Content-Disposition` 에 `attachment` 가 있다 |
| F7 | 업로드 보안 | (a) `evil.exe` 첨부 등록이 5xx 가 아니고, 글이 생겼더라도 그 첨부를 내려받았을 때 exe 내용이 아니다. (b) 11MB PDF 첨부 등록이 5xx 가 아니고 게시글이 생기지 않는다. F5 통과를 전제로 한다 |

## 품질

| ID | 항목 | 판정 |
|---|---|---|
| Q1 | Config 클래스 | `src/main/java` 에 `@Configuration` 이 붙고 `Board` 를 언급하는 클래스가 있다 (예: `EgovConfigBoardIdGeneration`). 정적 |
| Q2 | 페이징 | 글 12건을 등록한 뒤 `GET /egovBoardList.do` 에 12건이 **모두 나오지 않고**, `?pageIndex=2` 와 합치면 12건이 모두 나오며, 화면에 `pageIndex`/`pagination`/`paging` 문자열(페이지 이동 요소)이 있다 |
| Q3 | 검색 | 12건 중 하나의 제목에 들어 있는 고유 토큰으로 `?searchKeyword=토큰`(searchCondition 0 → 1 → 2 → 없음 순으로 시도) 요청 시 그 글은 나오고 다른 글은 나오지 않는다. 없는 검색어로 요청해도 오류 화면이 아니다 |
| Q4 | 트랜잭션 | `*Board*ServiceImpl.java` 에 `@Transactional` 이 있거나, `TransactionInterceptor`/`txAdvice` 를 정의한 설정 클래스의 `execution(...)` 포인트컷 패턴(`..`, `*` 해석)이 `패키지.클래스.insertBoard` 와 일치한다. 정적 |
| Q5 | ID 생성 | (a) Board 패키지 소스에서 `IdGnrService` 또는 `getNextStringId` 사용 (정적), (b) `POST /addBoardView.do` 등록 화면에 hidden/readonly 가 아닌 `name="boardId"` 입력란이 없다, (c) F1 에서 서버가 ID 를 채번했다 |
| Q6 | 예외/검증/i18n | (검증) 제목이 빈 등록 요청이 5xx 가 아니고 목록의 ID 집합이 늘어나지 않는다. (예외) 존재하지 않는 boardId 로 수정 화면 요청 시 응답 본문에 `Whitelabel Error Page` 나 Java 스택트레이스가 없다 (안내 화면이면 상태 코드가 500 이어도 통과). (i18n) 메시지 번들 base/ko/en 3종 모두에 `board` 가 들어간 키가 있고, board 화면 템플릿에 `#{...}` 메시지 표현식이 3개 이상 있다. 세 가지 모두 만족해야 통과 |

## 표준 준수

| ID | 항목 | 판정 |
|---|---|---|
| S1 | 표준프레임워크 적용률 | `egovframe-mcp-check` 의 `egov_check_compliance` 를 채점기가 stdio 로 직접 호출(AI 에 연결하지 않음). **위반 0건**이고, `*Board*(Controller|ServiceImpl|DAO|Mapper).java` 가 **3개 이상** 검사 대상에 포함되어야 한다. 적용률(%)은 결과에 함께 기록된다 |

검사 규칙(check MCP 기준): Controller 는 `@Controller`/`@RestController` + 매핑 어노테이션, ServiceImpl 은 `EgovAbstractServiceImpl` 상속 + `@Service` + `implements …Service`, DAO 는 `@Repository` + `EgovAbstractDAO`/`EgovAbstractMapper` 상속, Mapper 는 `@Mapper`/`@EgovMapper`.

## 결과 기록

- 실행마다 `results/human/<ID>/progress.jsonl` 에 한 줄(JSON)이 추가됩니다: 점수, 구분별 점수, 적용률, 항목별 통과 여부와 사유, 시작·종료 시각.
- `npm run collect` 는 최종 결과를 `results/human/<ID>/final.json` 에 따로 저장합니다.
- 진행자는 `npm run aggregate` 로 참가자별 도달 시각·그룹별 통계를 `results/human/report.md` 로 만듭니다.

## 기준 프로젝트의 기대 점수

| 대상 | 기대 결과 |
|---|---|
| `board-app` (게시판 없음) | B1 빌드만 통과 (목록 URL 이 없어 B2 는 404) → **1/16** |
| 진행자용 정답 프로젝트 (`npm run reference`, codegen 도구 2회 호출) | 기본·기능·표준 준수 전부 통과. 품질은 Q4(트랜잭션)만 실패 → **15/16** (codegen 이 `EgovConfigTransaction` 포인트컷을 넓히지 않기 때문. 사람이 한 줄 고치면 16/16) |
