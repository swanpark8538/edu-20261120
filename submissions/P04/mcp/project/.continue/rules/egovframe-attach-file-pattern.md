---
name: egovframe-attach-file-pattern
description: "전자정부 표준프레임워크(eGovFrame) 공통컴포넌트의 표준 첨부파일 패턴(`atchFileId` 그룹 키 + `COMTNFILE`(마스터) + `COMTNFILEDETAIL`(상세) 2테이블 모델)을 적용할 때 트리거. 게시판/FAQ/공지/신청서 등 어떤 도메인이든 \"파일 업로드\", \"첨부파일\", \"multipart\", \"file upload\", \"attach\", \"downFile\" 키워드가 등장하거나, 도메인 VO 에 첨부파일 필드를 추가해야 할 때 사용. 위반 시 결과: 공통컴포넌트와 호환 불가(다운로드/관리 화면이 새 테이블을 못 읽음), orphan 파일 누적, atchFileId 길이/형식 불일치로 FK 제약 위반. `EgovFileMngService` 의 정확한 메서드 시그니처, `FileVO` 의 필드, multipart 파라미터 명명, `egovFileIdGnrService` Bean 분리 등 위반 시 컴파일/런타임/데이터 무결성 어느 단계에서든 실패로 직결되는 invariants 만 모은 것."
alwaysApply: false
---

# eGovFrame 첨부파일 패턴 (atchFileId)

> **언제 적용:** 도메인에 첨부파일이 붙는 모든 화면(게시판·FAQ·공지·신청서·결재함·자료실 등) 의 **Controller / Service / VO / DDL / 매퍼 / JSP·Thymeleaf** 를 작성·수정·리뷰할 때.
> **적용 제외:** 단순 이미지 *프로필 사진처럼* 한 도메인당 1장만, 다운로드 화면도 없고 공통 관리 UI 와 연결 안 되는 케이스. 그 경우엔 도메인 테이블에 컬럼 두 개(파일경로/원본명)로 끝내도 무방.
> **요지:** eGov 의 첨부파일은 **`atchFileId` (CHAR(20)) 라는 그룹 키 한 개**로 1:N 파일을 묶는다. 도메인 VO 는 이 키 하나만 들고 있고, 실제 파일 메타는 `COMTNFILE`/`COMTNFILEDETAIL` 두 테이블이 보관한다. `EgovFileMngService` / `EgovFileMngUtil` 만이 이 키를 채번·삽입·갱신·삭제할 권한이 있다. 도메인 코드가 직접 UUID 만들거나 파일 경로 컬럼을 도메인 테이블에 두면 공통컴포넌트 관리 화면과 단절된다.

---

## Quick Reference

### 도메인 모델 (절대 바꾸지 말 것)

```
COMTNFILE (마스터)
├── ATCH_FILE_ID  CHAR(20) PK         ← "FILE_" + 15자리 = 20자 고정
├── CREAT_DT      DATETIME NOT NULL   (Oracle/Tibero: DATE / Postgres: TIMESTAMP)
└── USE_AT        CHAR(1)             ('Y'/'N')

COMTNFILEDETAIL (상세, 1:N)
├── ATCH_FILE_ID    CHAR(20)          ┐ 복합 PK
├── FILE_SN         NUMERIC(10)       ┘ (Oracle/Tibero: NUMBER(10))
├── FILE_STRE_COURS VARCHAR(2000)     ← 저장 경로 (Oracle/Tibero: VARCHAR2)
├── STRE_FILE_NM    VARCHAR(255)      ← 저장 파일명 (난수+timestamp)
├── ORIGNL_FILE_NM  VARCHAR(255)      ← 원본 파일명 (다운로드 시 사용)
├── FILE_EXTSN      VARCHAR(20)
├── FILE_CN         MEDIUMTEXT        (Oracle: CLOB / Postgres: TEXT)
├── FILE_SIZE       NUMERIC(8)
└── FK (ATCH_FILE_ID) REFERENCES COMTNFILE(ATCH_FILE_ID)
```

### 핵심 클래스 / Bean (외우기)

| 컴포넌트 | FQN / Bean name | 역할 |
|---|---|---|
| `EgovFileMngService` | `egovframework.com.cmm.service.EgovFileMngService` / **`@Resource(name="EgovFileMngService")`** | 메타 CRUD (DB) |
| `EgovFileMngUtil` | `egovframework.com.cmm.service.EgovFileMngUtil` / **`@Resource(name="EgovFileMngUtil")`** | multipart → `FileVO[]` 변환 + 실제 파일 디스크 쓰기 |
| `FileVO` | `egovframework.com.cmm.service.FileVO` | DTO (모든 필드 String) |
| `egovFileIdGnrService` | `org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl` (Bean name 고정) | `FILE_` prefix + 15자리 = 20자 atchFileId 채번 |
| `fileStrategy` | `EgovIdGnrStrategyImpl` (`prefix="FILE_"`, `cipers=15`, `fillChar='0'`) | 위 idgnr 의 전략 — **반드시 prefix/cipers 합이 20** |

### 8개 Invariants (위반 시 결과)

- [ ] **(1)** `atchFileId` 는 직접 만들지 않는다. **`EgovFileMngUtil.parseFileInf(...)` 또는 `EgovFileMngService.insertFileInfs(...)` 의 반환값**으로만 받는다.
  - 위반 시: `UUID.randomUUID()` 는 36자 → CHAR(20) PK 컬럼에 들어가지 않음. `Data too long for column` 또는 잘림으로 인한 FK 무결성 깨짐.
- [ ] **(2)** 도메인 VO 는 `String atchFileId` 한 개만 가진다. `filePath`, `fileName`, `fileSize` 같은 컬럼을 도메인 테이블에 직접 두지 않는다.
  - 위반 시: 공통컴포넌트 관리 UI / 다운로드 컨트롤러가 해당 도메인 첨부를 인식 못 함.
- [ ] **(3)** 컨트롤러 핸들러 인자는 `final MultipartHttpServletRequest multiRequest` 를 받고, 파일 파라미터명은 **`file_1`** (공통 컴포넌트 관용). `multiRequest.getFiles("file_1")` 로 `List<MultipartFile>` 추출.
- [ ] **(4)** **Insert 흐름은 반드시 2단계**: `parseFileInf(...)` (디스크에 파일 쓰고 `FileVO[]` 생성) → `insertFileInfs(fvoList)` (DB 에 INSERT, 반환값이 `atchFileId`). 둘 중 하나만 호출하면 orphan(디스크에만 있고 DB 에 없음) 또는 NPE(DB 에는 키가 있는데 디스크 파일 없음).
- [ ] **(5)** **Update 시 새 파일 추가**는 `getMaxFileSN(fvo)` 로 다음 FILE_SN 을 받아 `parseFileInf(files, key, cnt, atchFileId, "")` 의 **세번째 인자 `fileKeyParam` 에 넘겨야 한다**. 0 부터 다시 시작하면 복합 PK 충돌(`Duplicate entry`).
- [ ] **(6)** 도메인 삭제 시 `EgovFileMngService.deleteAllFileInf(fvo)` 를 **반드시 별도 호출**. 도메인 DELETE 만 하면 COMTNFILE/COMTNFILEDETAIL 에 orphan 누적 + 디스크 파일도 그대로.
- [ ] **(7)** `egovFileIdGnrService` Bean 은 일반 `egovIdGnrService` 와 **별도로 등록**되어야 한다 (prefix 가 다르므로). Bean name 은 정확히 `egovFileIdGnrService`, 시퀀스 테이블 row 의 `TABLE_NAME` 컬럼 값은 정확히 `FILE_ID`.
  - 위반 시: `NoSuchBeanDefinitionException: egovFileIdGnrService` 또는 다른 도메인의 시퀀스를 갉아먹어 다음 채번이 충돌.
- [ ] **(8)** 5.0 Boot 산출물은 CSRF 보호가 기본 활성이므로 업로드/첨부삭제 요청도 **CSRF 토큰을 포함**해야 한다. 생성 화면은 폼 hidden 필드(`id="csrfField"`, multipart 본문 파트로 전송)와 meta 태그 기반 fetch 헤더를 이미 갖추고 있다 — 커스텀 화면을 추가할 때 같은 패턴을 복제한다.
  - 위반 시: 업로드/삭제 POST 가 403 → `/egovCSRFAccessDenied.do` 화면. 파일이 `spring.servlet.multipart.max-file-size` 초과 시에도 파트 파싱 실패로 같은 화면이 나타날 수 있다 (`egovframe-troubleshooting` §2.13 참조).

---

## 정합 호출 순서 (실제 패턴)

### Insert (신규 등록 + 첨부)

```java
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import egovframework.com.cmm.service.EgovFileMngService;
import egovframework.com.cmm.service.EgovFileMngUtil;
import egovframework.com.cmm.service.FileVO;

import jakarta.annotation.Resource;

@Controller
public class FaqController {

    @Resource(name = "EgovFileMngService")   // ⚠️ 대문자 E 시작 (Bean name 그대로)
    private EgovFileMngService fileMngService;

    @Resource(name = "EgovFileMngUtil")
    private EgovFileMngUtil fileUtil;

    @PostMapping("/uss/olh/faq/insertFaqCn.do")
    public String insertFaqCn(final MultipartHttpServletRequest multiRequest,
                              @ModelAttribute("faqVO") FaqVO faqVO,
                              BindingResult bindingResult) throws Exception {

        // 1) 첨부파일 파싱 — 디스크에 저장하고 FileVO[] 만 생성 (아직 DB 미반영)
        String atchFileId = "";
        List<FileVO> fvoList = null;
        final List<MultipartFile> files = multiRequest.getFiles("file_1");  // ⚠️ 파라미터명 file_1

        if (files != null && !files.isEmpty()) {
            fvoList     = fileUtil.parseFileInf(files, "FAQ_", 0, "", "");
            //                                          ^^^^^  ^  ^   ^
            //                                          │      │  │   └ storePath: "" → Globals.fileStorePath
            //                                          │      │  └─── atchFileId: "" → 신규 채번
            //                                          │      └────── fileKeyParam: 0 (FILE_SN 시작값)
            //                                          └───────────── 저장 파일명 prefix (자유)
            atchFileId = fileMngService.insertFileInfs(fvoList);  // ⚠️ 반환값이 atchFileId
        }

        // 2) 도메인 VO 에 atchFileId 만 세팅 후 도메인 INSERT
        faqVO.setAtchFileId(atchFileId);
        egovFaqService.insertFaq(faqVO);

        return "redirect:/uss/olh/faq/selectFaqList.do";
    }
}
```

### Update (기존 도메인에 파일 추가 / 첨부만 신규 생성)

```java
@PostMapping("/uss/olh/faq/updateFaqCn.do")
public String updateFaqCn(final MultipartHttpServletRequest multiRequest,
                          @ModelAttribute("faqVO") FaqVO faqVO) throws Exception {

    String atchFileId = faqVO.getAtchFileId();   // 기존 도메인이 들고 있던 그룹 키
    final List<MultipartFile> files = multiRequest.getFiles("file_1");

    if (files != null && !files.isEmpty()) {
        if (atchFileId == null || "".equals(atchFileId)) {
            // (A) 첨부 그룹이 아직 없음 → 신규 채번
            List<FileVO> result = fileUtil.parseFileInf(files, "FAQ_", 0, atchFileId, "");
            atchFileId = fileMngService.insertFileInfs(result);
            faqVO.setAtchFileId(atchFileId);
        } else {
            // (B) 기존 그룹에 파일 추가 → 다음 FILE_SN 부터 시작 (중복 PK 방지)
            FileVO fvo = new FileVO();
            fvo.setAtchFileId(atchFileId);
            int cnt = fileMngService.getMaxFileSN(fvo);   // ⚠️ 반드시 호출
            List<FileVO> fvoList = fileUtil.parseFileInf(files, "FAQ_", cnt, atchFileId, "");
            fileMngService.updateFileInfs(fvoList);       // ⚠️ insert 아닌 update*Infs*
        }
    }

    egovFaqService.updateFaq(faqVO);
    return "redirect:/uss/olh/faq/selectFaqList.do";
}
```

### Delete (도메인 삭제 시 첨부 그룹도 같이)

```java
@PostMapping("/uss/olh/faq/deleteFaqCn.do")
public String deleteFaqCn(@ModelAttribute("faqVO") FaqVO faqVO) throws Exception {

    String atchFileId = faqVO.getAtchFileId();

    // 1) 도메인 먼저 삭제
    egovFaqService.deleteFaq(faqVO);

    // 2) 첨부 그룹 전체 삭제 — orphan 방지를 위해 *반드시* 별도 호출
    if (atchFileId != null && !atchFileId.isEmpty()) {
        FileVO fvo = new FileVO();
        fvo.setAtchFileId(atchFileId);
        fileMngService.deleteAllFileInf(fvo);   // ⚠️ deleteAllFileInf (그룹 단위)
    }
    return "redirect:/uss/olh/faq/selectFaqList.do";
}
```

### 개별 파일 1건만 삭제 (목록에서 X 버튼)

```java
FileVO fvo = new FileVO();
fvo.setAtchFileId(atchFileId);
fvo.setFileSn(fileSn);                 // 복합 PK 의 두 번째 컬럼까지 지정
fileMngService.deleteFileInf(fvo);     // 그룹은 살아 있고 1행만 제거
```

### 목록 조회 (상세 화면에서 첨부 목록 렌더링)

```java
FileVO fvo = new FileVO();
fvo.setAtchFileId(faqVO.getAtchFileId());
List<FileVO> attachList = fileMngService.selectFileInfs(fvo);
model.addAttribute("attachList", attachList);
// JSP/Thymeleaf 는 attachList 를 돌면서 streFileNm/orignlFileNm 으로 다운로드 URL 구성
```

### 다운로드 (공통 컴포넌트 다운로드 컨트롤러 활용)

별도로 다운로드 컨트롤러를 새로 만들지 않는다. `egovframework.com.cmm.web.EgovFileDownloadController` 또는 `EgovFileMngUtil#downFile(response, streFileNm, orignFileNm)` 를 사용. 직접 `FileInputStream` + `response.getOutputStream()` 으로 짜면 KISA 시큐어코딩 점검(경로 traversal) 을 우회한 코드가 되어 보안 검수에서 막힌다.

---

## FileVO 필드 매핑 (혼동 금지)

```java
public class FileVO extends ComDefaultVO {
    public String atchFileId    = "";   // 그룹 키 (COMTNFILE.ATCH_FILE_ID)
    public String creatDt       = "";   // COMTNFILE.CREAT_DT
    public String fileSn        = "";   // COMTNFILEDETAIL.FILE_SN (String — 절대 int 로 바꾸지 말 것)
    public String fileStreCours = "";   // COMTNFILEDETAIL.FILE_STRE_COURS (저장 경로)
    public String streFileNm    = "";   // COMTNFILEDETAIL.STRE_FILE_NM   (저장 파일명 = prefix+timestamp+seq)
    public String orignlFileNm  = "";   // COMTNFILEDETAIL.ORIGNL_FILE_NM (사용자가 본 파일명)
    public String fileExtsn     = "";   // COMTNFILEDETAIL.FILE_EXTSN
    public String fileMg        = "";   // COMTNFILEDETAIL.FILE_SIZE      (이름 주의: fileMg = file Megabyte 아님 — 그냥 사이즈 바이트)
    public String fileCn        = "";   // COMTNFILEDETAIL.FILE_CN        (텍스트 내용 — 보통 빈값)
}
```

**모든 필드가 String** 인 점이 함정이다. 새로 만든 도메인 VO 에서 `Long fileSize` 를 받기 좋다고 타입 바꾸면 MyBatis `resultType` 매핑이 깨진다.

---

## DDL 벤더 차이 매트릭스 (`COMTNFILE` / `COMTNFILEDETAIL`)

| 컬럼 | MySQL/MariaDB | Oracle/Tibero | PostgreSQL | CUBRID | Altibase/Goldilocks |
|---|---|---|---|---|---|
| `ATCH_FILE_ID` | `CHAR(20)` | `CHAR(20)` | `CHAR(20)` | `CHAR(20)` | `CHAR(20)` |
| `CREAT_DT` | `DATETIME` | `DATE` | `TIMESTAMP` | `DATETIME` | `DATE` |
| `FILE_SN` | `NUMERIC(10)` | `NUMBER(10)` | `NUMERIC(10)` | `NUMERIC(10)` | `NUMBER(10)` |
| `FILE_STRE_COURS` | `VARCHAR(2000)` | `VARCHAR2(2000)` | `VARCHAR(2000)` | `VARCHAR(2000)` | `VARCHAR2(2000)` |
| `FILE_CN` | `MEDIUMTEXT` | `CLOB` | `TEXT` | `STRING` | `CLOB` |
| FK 문법 | `FOREIGN KEY <name> (col) REFERENCES …` | `CONSTRAINT <name> FOREIGN KEY …` | `FOREIGN KEY (col) REFERENCES …` | (Oracle 스타일) | (Oracle 스타일) |

DDL 정규화 시 `VARCHAR2` ↔ `VARCHAR`, `NUMBER` ↔ `NUMERIC`, `DATE` ↔ `DATETIME` 변환만 해도 대부분 호환되지만 **`MEDIUMTEXT` / `CLOB` / `TEXT` 는 의미가 같지 않음 — 벤더별 명시적 매핑 필요**.

---

## Bean 설정 (Boot 5.0 / Java Config)

```java
// EgovConfigIdGeneration.java 또는 그에 준하는 @Configuration 클래스에 추가
@Bean(name = "egovFileIdGnrService", destroyMethod = "destroy")
public EgovTableIdGnrServiceImpl egovFileIdGnrService(DataSource dataSource) throws Exception {
    EgovTableIdGnrServiceImpl bean = new EgovTableIdGnrServiceImpl();
    bean.setDataSource(dataSource);
    bean.setStrategy(fileStrategy());
    bean.setBlockSize(10);
    bean.setTable("COMTECOPSEQ");        // ⚠️ 시퀀스 관리 테이블 (eGov 표준)
    bean.setTableName("FILE_ID");        // ⚠️ 이 row 값으로 다른 도메인 시퀀스와 분리
    return bean;
}

@Bean(name = "fileStrategy")
public EgovIdGnrStrategyImpl fileStrategy() {
    EgovIdGnrStrategyImpl strategy = new EgovIdGnrStrategyImpl();
    strategy.setPrefix("FILE_");         // ⚠️ "FILE_" + 15자리 = 정확히 20자 (CHAR(20) 와 일치)
    strategy.setCipers(15);
    strategy.setFillChar('0');
    return strategy;
}
```

**Prefix(5) + cipers(15) = 20 등식이 깨지면 PK 컬럼 길이와 불일치** — `Data truncation` 또는 `value too long for type character(20)` 발생.

스캐폴드가 시퀀스 관리 테이블 `COMTECOPSEQ` 와 첨부파일 두 테이블(`COMTNFILE`, `COMTNFILEDETAIL`) 시드 SQL 을 같이 만들지 않으면 첫 업로드에서 `Table 'xxx.COMTECOPSEQ' doesn't exist` 가 난다. seed 에 반드시 포함.

---

## Service / Mapper 작성 시 주의

도메인 Service 가 `EgovFileMngService` 를 호출할 때:
- 가능하면 **Controller 에서 직접 호출**한다 (공통컴포넌트가 그렇게 짜여 있음). Service 안으로 깊이 끌어들이면 트랜잭션 경계가 첨부 디스크 I/O 와 묶여서 롤백/일관성 처리가 복잡해진다.
- 굳이 Service 에서 호출해야 하면 `@Transactional` 메서드 *밖에서* 디스크 write(`parseFileInf`) 가 끝나도록 분리 — 그렇지 않으면 DB 롤백 후에도 디스크 파일은 남는다.

매퍼 SQL 을 직접 작성할 필요 없음 — `EgovFileMngService` 가 이미 매퍼(`FileManageDAO` / `FileManageMapper`)를 들고 있다. 도메인 매퍼에는 `atchFileId` 컬럼만 SELECT/INSERT/UPDATE 하면 끝.

---

## 자주 틀리는 함정 (Don't)

```java
// ❌ 자체 UUID 채번
faqVO.setAtchFileId(UUID.randomUUID().toString());   // 36자 → CHAR(20) PK 들어가지 않음

// ❌ 도메인 테이블에 파일 경로 직접 보관
CREATE TABLE FAQ (
    FAQ_ID VARCHAR(36) PRIMARY KEY,
    FILE_PATH VARCHAR(500),     // ← 이게 있으면 공통 다운로드 UI 와 단절
    FILE_NAME VARCHAR(255)
);

// ❌ insertFileInf (단수) 을 멀티 업로드에 사용
for (MultipartFile f : files) {
    FileVO vo = new FileVO();
    // ... 수동 세팅
    fileMngService.insertFileInf(vo);   // 매 호출마다 새 atchFileId 채번 → N 개 그룹이 생김
}

// ❌ parseFileInf 만 호출하고 insertFileInfs 누락
List<FileVO> fvoList = fileUtil.parseFileInf(files, "FAQ_", 0, "", "");
faqVO.setAtchFileId(fvoList.get(0).getAtchFileId());   // 디스크엔 있지만 DB 엔 없음 → 다운로드 시 NotFound

// ❌ Update 에서 FILE_SN 을 0 부터 다시 시작
List<FileVO> fvoList = fileUtil.parseFileInf(files, "FAQ_", 0, atchFileId, "");
fileMngService.updateFileInfs(fvoList);   // Duplicate entry on PK (ATCH_FILE_ID, FILE_SN)

// ❌ 도메인 삭제만 하고 첨부 그룹 안 지움
egovFaqService.deleteFaq(faqVO);
// → COMTNFILE/COMTNFILEDETAIL 에 orphan 누적, 디스크 파일도 영구히 남음

// ❌ 다운로드 컨트롤러 직접 구현
@GetMapping("/download")
public void download(@RequestParam String path, HttpServletResponse response) {
    FileInputStream fis = new FileInputStream(path);   // ← path traversal 취약
    ...
}
// → KISA 시큐어코딩 진단에서 자동 차단. EgovFileMngUtil.downFile() 사용.

// ❌ multipart 파라미터명 임의 지정
final List<MultipartFile> files = multiRequest.getFiles("attachment");
// → 공통 컴포넌트 화면 fragment 와 안 맞음. 표준은 "file_1"
//   (반드시 "file_1" 이어야 한다는 RTE 제약은 없지만, 공통 화면 fragment 를 재사용하려면 일치 필요)

// ❌ FileVO 필드 타입 변경
public class FileVO {
    public Long fileSize;     // ← MyBatis resultType 매핑 깨짐. String 그대로 유지.
    public int  fileSn;       // ← 동일 문제
}

// ❌ FILE_ID prefix 변경
strategy.setPrefix("ATCH_");  // 5자
strategy.setCipers(15);
// → "ATCH_" + 15 = 20 으로 길이는 맞지만, COMTECOPSEQ 의 기존 row 와 prefix 가 어긋나
//   재기동 후 시퀀스가 처음부터 다시 시작 → 운영 데이터와 충돌 가능
```

---

## 신규 프로젝트 생성 직후 점검

- [ ] `COMTECOPSEQ` 테이블 존재 (시퀀스 관리)
- [ ] `COMTNFILE`, `COMTNFILEDETAIL` 테이블 존재 (시드 DDL 에 포함)
- [ ] `egovFileIdGnrService` + `fileStrategy` Bean 등록 (Java Config 또는 XML)
- [ ] `Globals.fileStorePath` 프로퍼티에 실제 저장 경로 지정 (`application.properties` 또는 `globals.properties`)
- [ ] 위 경로 디렉터리 쓰기 권한 확인 (`EgovFileMngUtil` 이 mkdirs 시도하지만 권한 없으면 실패)
- [ ] `EgovFileMngService` Bean 등록 확인 (`EgovFileMngServiceImpl` 이 `@Service("EgovFileMngService")` 로 등록되어 있는지)
- [ ] `EgovFileMngUtil` Bean 등록 확인 (`@Component("EgovFileMngUtil")`)
- [ ] 도메인 VO 에 `atchFileId` 필드 1개만 (file_path/file_name 직접 컬럼 X)
- [ ] 도메인 매퍼의 INSERT/UPDATE 가 `atchFileId` 를 포함
- [ ] 도메인 삭제 매퍼와 함께 `deleteAllFileInf` 호출 코드가 같은 Service 메서드 안에 있는지
