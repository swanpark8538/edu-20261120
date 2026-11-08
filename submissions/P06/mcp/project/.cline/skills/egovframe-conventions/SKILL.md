---
name: egovframe-conventions
description: "전자정부 표준프레임워크(eGovFrame) 5.x / Spring Boot 3.x / Java 17 / Jakarta EE 프로젝트의 Java 코드(Controller·Service·DAO·VO·Spring 설정·pom.xml 의존성)를 작성·수정·리뷰할 때 적용. 위반 시 컴파일 실패 또는 기동 실패로 직결되는 invariants 모음 — `@EgovMapper` (NOT `@Mapper`), `@EgovNullCheck` String 전용, `jakarta.*` (NOT `javax.*`), `EgovTableIdGnrServiceImpl` (NOT `EgovTableIdGnrService`), `ExceptionHandlerService` 는 `.manager` 하위, Bootstrap Bean 12종 수동 등록, eGovFrame Boot Parent 설정, RTE artifactId 네이밍 등. \"표준프레임워크\", \"egovframe\", \"전자정부 프레임워크\" 키워드 또는 `org.egovframe.rte.*` import 가 있는 코드 작업 시 트리거."
---

# eGovFrame Coding Conventions (v5.0)

> **언제 적용:** `org.egovframe.rte.*` 를 import 하는 **Java 코드**(Controller / Service / DAO / VO / Spring 설정 / pom.xml 의존성)를 작성·수정·리뷰할 때.
> **적용 제외:** 프론트엔드(JSP/Thymeleaf 자체), `egovframework.rte.*`(3.10 이하 패키지), 일반 Spring 모범사례(별도로 알고 있다고 가정).
> **요지:** eGovFrame 5.0 은 **Spring Boot 3.x / Spring 6.x / Java 17 / Jakarta EE 9+** 위에서 동작하며, RTE 의 artifactId·패키지 네이밍·일부 클래스명이 4.x 와 다르다. 본 룰은 **위반 시 컴파일 실패 또는 기동 실패로 직결되는 invariants** 만 모은 것 — 패턴 설명·예제 코드는 scaffold 가 생성한 실제 파일을 참고하거나 별도 `docs/egovframe-architecture.md` 문서를 참조한다.

---

## Quick Reference

### Bootstrap Bean 12종 (필수 — 빠지면 기동 실패)

`EgovAbstractServiceImpl` 또는 `EgovPropertyServiceImpl` 를 쓰는 Boot 프로젝트는 다음 Bean 들을 반드시 `@Configuration` 클래스에 수동 등록한다. v5.0 공식 템플릿은 단일 클래스가 아닌 **9개 책임별 `@Configuration` 으로 분할** 후 `@Import` 로 결합한다.

```
egovframework.example.config/
├── EgovConfigCommon.java       # MessageSource, LeaveaTrace, AntPathMatcher
├── EgovConfigProperties.java   # EgovPropertyServiceImpl
├── EgovConfigDatasource.java   # DataSource (HikariCP / EmbeddedDatabase)
├── EgovConfigMapper.java       # SqlSessionFactoryBean, @MapperScan
├── EgovConfigIdGeneration.java # EgovIdGnrStrategyImpl, EgovTableIdGnrServiceImpl
├── EgovConfigTransaction.java  # TransactionInterceptor, Advisor (Java Config)
├── EgovConfigAspect.java       # ExceptionTransfer, EgovAopExceptionTransfer
├── EgovConfigValidation.java   # LocalValidatorFactoryBean (messageSource 결합)
├── EgovConfigWebSecurity.java  # Spring Security 필터 체인 (보안 헤더·CSRF·방화벽·CORS)
└── EgovConfigWeb.java          # WebMvcConfigurer + @Import 로 위 9개 결합
```

체크리스트 (이 Bean 들이 누락되면 `NoSuchBeanDefinitionException` / `BeanCreationException` 발생):

- [ ] `antPathMatcher` — 공용 `AntPathMatcher` Bean
- [ ] `defaultTraceHandler` — `LeaveaTrace` 의 핸들러
- [ ] `traceHandlerService` — `DefaultTraceHandleManager`
- [ ] `leaveaTrace` — `EgovAbstractServiceImpl` 필수 의존
- [ ] `messageSource` — 다중 basename (앱 메시지 + RTE 내장)
- [ ] `messageSourceAccessor` — `EgovPropertyServiceImpl` 등이 사용
- [ ] `propertiesService` — `pageUnit`/`pageSize` 등 전역 프로퍼티
- [ ] `egovIdGnrService` — PK 생성 (`EgovTableIdGnrServiceImpl`)
- [ ] `dataSource` — DataSource Bean
- [ ] `sqlSession` — `SqlSessionFactoryBean` (Bean name 주의: DAO 방식 필요시 `sqlSession`)
- [ ] `sqlSessionTemplate` — `@MapperScan` 인터페이스 매퍼용
- [ ] `txManager` / `txAdvice` / `txAdvisor` — Java Config 기반 트랜잭션
- [ ] Validator — `LocalValidatorFactoryBean` (messageSource 결합)
- [ ] `EgovKrdsPaginationRenderer` + `EgovPaginationDialect` — 페이징 UI

### 핵심 클래스 Fully-Qualified Import (자주 헷갈리는 곳)

| 클래스 | 전체 경로 | v4.3 와의 차이 / 함정 |
|--------|-----------|----------------|
| `EgovAbstractServiceImpl` | `org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl` | 동일 |
| `LeaveaTrace` | `org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace` | 동일 |
| `EgovPropertyServiceImpl` | `org.egovframe.rte.fdl.property.impl.EgovPropertyServiceImpl` | 동일 |
| `EgovIdGnrService` | `org.egovframe.rte.fdl.idgnr.EgovIdGnrService` | 동일 |
| **`EgovTableIdGnrServiceImpl`** | `org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl` | ⚠️ **v4.x 는 `EgovTableIdGnrService`** (`Impl` 없음) |
| `EgovIdGnrStrategyImpl` | `org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl` | ⚠️ `.strategy` 하위 |
| `EgovMap` | `org.egovframe.rte.psl.dataaccess.util.EgovMap` | 동일 |
| **`@EgovMapper`** | `org.egovframe.rte.psl.dataaccess.mapper.EgovMapper` | ⚠️ **v4.x 는 `Mapper`** — 매퍼 인터페이스 어노테이션은 반드시 `@EgovMapper` |
| `PaginationInfo` | `org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo` | 동일 |
| **`AbstractKrdsPaginationRenderer`** | `org.egovframe.rte.ptl.mvc.tags.ui.pagination.AbstractKrdsPaginationRenderer` | ⚠️ **v5.0 신규** |
| `ExceptionTransfer` | `org.egovframe.rte.fdl.cmmn.aspect.ExceptionTransfer` | 동일 |
| `ExceptionHandler` | `org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler` | `.handler` 하위 |
| **`ExceptionHandlerService`** | `org.egovframe.rte.fdl.cmmn.exception.**manager**.ExceptionHandlerService` | ⚠️ **`.handler` 아님 — `.manager` 하위** (자주 헷갈림) |
| `DefaultExceptionHandleManager` | `org.egovframe.rte.fdl.cmmn.exception.manager.DefaultExceptionHandleManager` | `.manager` 하위 |
| `EgovNullCheck` | `org.egovframe.rte.ptl.reactive.validation.EgovNullCheck` | ⚠️ **v5.0 신규 — String 필드 전용** (아래 Validation Layer 참고) |
| `@Resource` | `jakarta.annotation.Resource` | ⚠️ **v4.x 는 `javax.*`** |
| `@PostConstruct` | `jakarta.annotation.PostConstruct` | ⚠️ **v4.x 는 `javax.*`** |
| `@Valid` | `jakarta.validation.Valid` | ⚠️ **v4.x 는 `javax.*`** |
| `SpringTemplateEngine` | `org.thymeleaf.spring6.SpringTemplateEngine` | ⚠️ **v4.x 는 `spring5`** |

---

## Controller Layer

**Target:** `@Controller`, `@RestController`. Servlet 직접 사용은 Spring MVC 가 부적합하고 그 사유가 명확할 때만 허용.

**Rules:**
1. 핸들러는 Spring MVC 어노테이션으로 매핑한다.
2. DAO 클래스를 직접 주입/호출하지 않는다. `SqlMapClientDaoSupport`, `SqlSessionDaoSupport`, `HibernateDaoSupport` 의 서브클래스도 동일하게 금지.
3. Controller 에서 NoSQL, MQ, 캐시에 직접 접근하지 않는다.
4. 비즈니스 로직은 주입된 Service **인터페이스**를 통해서만 호출한다.
5. 의존성 주입은 `jakarta.annotation.Resource` 또는 생성자 주입을 사용한다 (`javax.annotation.Resource` 는 Spring Boot 3 에서 제거됨).

```java
// ✅ Correct
@Controller
@RequestMapping("/sample")
public class SampleController {

    @Resource(name = "sampleService")  // jakarta.annotation.Resource
    private SampleService sampleService;

    @GetMapping("/list")
    public String list(Model model) throws Exception {
        model.addAttribute("list", sampleService.selectList());
        return "sample/list";
    }
}

// ❌ DAO 직접 주입 — 금지
@Controller
public class SampleController {
    @Resource(name = "sampleMapper")
    private SampleMapper sampleMapper; // ❌ Service 를 주입하라
}

// ❌ Controller 안에서 비즈니스 로직 처리 — 금지
@GetMapping("/calc")
public String calc(Model model) {
    int result = heavyBusinessCalculation(); // ❌ Service 로 위임하라
    model.addAttribute("result", result);
    return "sample/result";
}

// ❌ javax.annotation.Resource 사용 — Boot 3.x 에서 제거됨
import javax.annotation.Resource; // ❌
```

---

## Service Layer

**Target:** `@Service` 또는 `@Component` 비즈니스 클래스(테스트 제외).

**Rules:**
1. 모든 Service 구현체는 `EgovAbstractServiceImpl` 을 (직접 또는 베이스 클래스 경유로) 상속해야 한다.
2. 모든 Service 구현체는 전용 Service **인터페이스**를 구현해야 한다.
3. `processException("코드")` 헬퍼는 부모(`EgovAbstractServiceImpl`)가 제공한다 — `EgovBizException` 을 직접 throw 하지 말고 이 헬퍼를 사용한다.

**왜 인터페이스가 필수인가:** eGovFrame AOP 는 CGLIB 가 아니라 **Java Dynamic Proxy** 를 쓴다. 인터페이스 없이 클래스만 있으면 프록시 자체가 불가능해서 트랜잭션·로깅 AOP 가 동작하지 않는다.

```java
// ✅ Correct — 인터페이스 + 구현 페어
public interface SampleService {
    List<?> selectSampleList(SampleVO vo) throws Exception;
    SampleVO selectSample(SampleVO vo) throws Exception;
}

@Service("sampleService")
public class SampleServiceImpl extends EgovAbstractServiceImpl implements SampleService {

    @Resource(name = "sampleMapper")  // jakarta.annotation.Resource
    private SampleMapper sampleMapper;

    @Override
    public List<?> selectSampleList(SampleVO vo) throws Exception {
        return sampleMapper.selectSampleList(vo);
    }

    @Override
    public SampleVO selectSample(SampleVO vo) throws Exception {
        SampleVO resultVO = sampleMapper.selectSample(vo);
        if (resultVO == null) {
            throw processException("info.nodata.msg"); // 부모 헬퍼 활용
        }
        return resultVO;
    }
}

// ❌ 인터페이스 없음 — Java Proxy 불가
@Service
public class SampleService extends EgovAbstractServiceImpl { }

// ❌ EgovAbstractServiceImpl 미상속
@Service
public class SampleServiceImpl implements SampleService { }
```

---

## DAO / Data Access Layer

**Target:** `@Repository` 클래스 및 매퍼/리포지토리 코드.

| 기술 | 필수 패턴 |
|------|----------|
| iBatis (legacy) | `extends EgovAbstractDAO` |
| MyBatis 클래스 매퍼 | `extends EgovAbstractMapper` |
| MyBatis 인터페이스 매퍼 | **`@EgovMapper("name")`** (eGovFrame 5.0) + `@MapperScan` |
| JPA | `extends JpaRepository`, `CrudRepository`, 또는 `PagingAndSortingRepository` |
| JPA 대안 | `EntityManager` 를 직접/베이스 클래스로 주입 |

**금지:** `SqlSessionDaoSupport` 의 `insert`, `delete`, `update`, `select`, `list` 직접 호출.

**v5.0 매퍼 어노테이션 변경:**
- v4.x: `@Mapper("name")` from `org.egovframe.rte.psl.dataaccess.mapper.Mapper`
- **v5.0: `@EgovMapper("name")` from `org.egovframe.rte.psl.dataaccess.mapper.EgovMapper`**
- 클래스명·import 경로가 모두 바뀌었다. 단순 import 변경만으로는 동작하지 않는다.

```java
// ✅ MyBatis 인터페이스 매퍼 (v5.0)
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

@EgovMapper("sampleMapper")
public interface SampleMapper {
    List<?> selectSampleList(SampleVO vo) throws Exception;
    int selectSampleListTotCnt(SampleVO vo);
}

// ✅ MyBatis 클래스 매퍼
@Repository
public class SampleDAO extends EgovAbstractMapper {
    public List<SampleVO> selectList(SampleVO vo) {
        return selectList("sample.selectList", vo);
    }
}

// ✅ JPA
public interface SampleRepository extends JpaRepository<Sample, Long> {
    List<Sample> findByStatus(String status);
}

// ❌ v4.x @Mapper 사용 — v5.0 에서 EgovMapper 로 교체
import org.egovframe.rte.psl.dataaccess.mapper.Mapper;
@Mapper("sampleMapper") // ❌ v5.0 은 @EgovMapper

// ❌ SqlSessionDaoSupport 직접 호출 — 금지
@Repository
public class SampleDAO extends SqlSessionDaoSupport {
    public List<SampleVO> selectList(SampleVO vo) {
        return getSqlSession().selectList("sample.selectList", vo); // ❌
    }
}
```

---

## Configuration Layer

**Target:** `@Configuration` Java Config (XML 설정은 v5.0 권장 외).

**v5.0 표준 패턴:** 설정을 **단일 모놀리식 클래스가 아닌 책임별 9개 파일로 분할**한다 (`@Import` 로 결합). 각 파일은 단일 책임만 가진다.

| Config 클래스 | 책임 |
|--------------|------|
| `EgovConfigCommon` | `LeaveaTrace`, `MessageSource`, `MessageSourceAccessor`, `AntPathMatcher`, `DefaultTraceHandler`, `DefaultTraceHandleManager` |
| `EgovConfigProperties` | `EgovPropertyServiceImpl` (Map 또는 파일 기반) |
| `EgovConfigDatasource` | `DataSource` (HikariCP / DBCP / EmbeddedDatabase) |
| `EgovConfigMapper` | `SqlSessionFactoryBean`, `SqlSessionTemplate`, `@MapperScan` |
| `EgovConfigIdGeneration` | `EgovIdGnrStrategyImpl`, `EgovTableIdGnrServiceImpl` |
| `EgovConfigTransaction` | `DataSourceTransactionManager`, `TransactionInterceptor`, `Advisor` |
| `EgovConfigAspect` | `ExceptionTransfer`, `EgovAopExceptionTransfer`, `DefaultExceptionHandleManager` |
| `EgovConfigValidation` | `LocalValidatorFactoryBean` (messageSource 와 결합) |
| `EgovConfigWebSecurity` | Spring Security `SecurityFilterChain` — 보안 헤더·CSRF·StrictHttpFirewall·CORS (`egov.security.*`) |
| `EgovConfigWeb` | `WebMvcConfigurer` 구현, Thymeleaf 엔진, ResourceHandlers, 위 Config 들을 `@Import` |

**필수 포함 사항:**
- 트랜잭션 처리: `@Transactional` 또는 Java Config 의 `TransactionInterceptor` + `Advisor`
- 커넥션 풀: HikariCP(권장), DBCP2, 또는 `EmbeddedDatabaseBuilder` (개발/테스트)

```java
// ✅ v5.0 표준 — 메인 Web Config 가 다른 설정들을 Import
@Configuration
@Import({
    EgovConfigAspect.class,
    EgovConfigCommon.class,
    EgovConfigDatasource.class,
    EgovConfigIdGeneration.class,
    EgovConfigMapper.class,
    EgovConfigProperties.class,
    EgovConfigTransaction.class,
    EgovConfigValidation.class,
    EgovConfigWebSecurity.class
})
public class EgovConfigWeb implements WebMvcConfigurer, ApplicationContextAware { ... }
```

```java
// ❌ 모든 Bean 을 단일 @Configuration 클래스에 몰아넣기 — v5.0 권장 패턴 위반
@Configuration
public class EgovConfigWeb {
    @Bean public LeaveaTrace leaveaTrace() { ... }
    @Bean public DataSource dataSource() { ... }
    @Bean public TransactionInterceptor txAdvice() { ... }
    @Bean public SqlSessionFactoryBean sqlSession() { ... }
    // ... 20+ Bean — 책임별로 분리하라
}
```

---

## Validation Layer (VO Annotations)

**Target:** `@Controller` / `@RequestBody` 입력 검증 (`@Valid` 가 trigger 하는 VO/DTO 의 필드 어노테이션).

**Rules:**
1. **`@EgovNullCheck` 는 반드시 `String` 필드에만 붙인다.** 이 어노테이션의 ConstraintValidator 는 `String` 타입만 처리하도록 구현되어 있다 — `Timestamp` / `Date` / `LocalDate` / `LocalDateTime` / `Long` / `Integer` / `BigInteger` / `BigDecimal` / `Boolean` / enum / 컬렉션 등 **비-String 타입에 붙이면 Bean 초기화 시점에 `HV000030: No validator could be found for constraint ...` 가 발생해 컨텍스트 자체가 기동 실패**한다. (자세한 증상/복구는 troubleshooting 의 2.12 참고)
2. 비-String 필수 검증은 `jakarta.validation.constraints.NotNull` 을 사용한다. 숫자 범위는 `@Min` / `@Max`, 날짜는 `@PastOrPresent` / `@FutureOrPresent` 등 jakarta 표준과 조합한다.
3. `javax.validation.*` 은 절대 import 하지 않는다 (Boot 3.x 에서 `jakarta.validation.*` 로 대체됨 — Compatibility Layer 의 jakarta 규칙과 동일).

```java
// ✅ Correct — 타입별로 어노테이션을 구분
import org.egovframe.rte.ptl.reactive.validation.EgovNullCheck;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public class SampleVO extends SampleDefaultVO {

    @EgovNullCheck(message = "{confirm.required.name}")
    private String name;                       // ✅ String → @EgovNullCheck

    @EgovNullCheck(message = "{confirm.required.useYn}")
    private String useYn;                      // ✅ String("Y"/"N") → @EgovNullCheck

    @NotNull(message = "{confirm.required.createdAt}")
    private Timestamp createdAt;               // ✅ 비-String → @NotNull

    @NotNull(message = "{confirm.required.amount}")
    @Min(value = 0, message = "{validation.amount.min}")
    private BigDecimal amount;                 // ✅ 비-String → @NotNull (+ 범위 제약)

    @NotNull(message = "{confirm.required.deptId}")
    private BigInteger deptId;                 // ✅ 비-String → @NotNull
}

// ❌ Wrong — 기동 시점에 HV000030 으로 컨텍스트 로딩 실패
public class SampleVO extends SampleDefaultVO {
    @EgovNullCheck(message = "{confirm.required.createdAt}")
    private Timestamp createdAt;               // ❌ 비-String

    @EgovNullCheck(message = "{confirm.required.deptId}")
    private BigInteger deptId;                 // ❌ 비-String

    @EgovNullCheck(message = "{confirm.required.useYn}")
    private Boolean useYn;                     // ❌ Boolean (String("Y"/"N") 이면 OK)
}
```

> **자동 생성 도구와의 정합성:** `egov_scaffold_project` / `egov_add_entity` 가 만든 VO 는 위 규칙대로 String 필드에만 `@EgovNullCheck` 를 붙인다. **AI 에이전트가 사후에 VO 를 편집·확장할 때 비-String 필드에 동일 어노테이션을 잘못 추가하지 않도록 주의** — 가장 흔한 사고가 `Timestamp createdAt` / `BigInteger id` / `Boolean useYn` 같은 컬럼에 `@EgovNullCheck` 를 일괄 적용해 기동을 망가뜨리는 경우다.

---

## Library and Extension Rules

**eGovFrame Boot Parent 설정** — Spring Boot 기반 프로젝트는 반드시 v5.0 Boot Parent 를 사용:

```xml
<!-- ✅ pom.xml 최상단 parent 설정 (v5.0) -->
<parent>
    <groupId>org.egovframe.boot</groupId>
    <artifactId>egovframe-boot-starter-parent</artifactId>
    <version>5.0.0</version>
</parent>
```

> **artifactId 변경 주의 (v4.3 → v5.0):**
> - v4.3: `org.egovframe.boot.starter.parent`
> - v5.0: `egovframe-boot-starter-parent`
>
> 점 → 하이픈, `org.` 접두 제거. parent 만 바꾸고 자식 의존성 artifactId 를 그대로 두면 **dependency resolution 실패**.

**필수 의존성 (pom.xml)** — v5.0 Boot Parent 가 RTE 모듈의 버전을 관리하므로 `<version>` 태그를 명시하지 않는다:

```xml
<repositories>
    <repository>
        <id>egovframe</id>
        <url>https://maven.egovframe.go.kr/maven/</url>
        <releases><enabled>true</enabled></releases>
        <snapshots><enabled>false</enabled></snapshots>
    </repository>
</repositories>

<dependencies>
    <!-- 표준프레임워크 실행환경 (v5.0 artifactId — egovframe-rte-* 형식) -->
    <dependency>
        <groupId>org.egovframe.rte</groupId>
        <artifactId>egovframe-rte-ptl-mvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.egovframe.rte</groupId>
        <artifactId>egovframe-rte-psl-dataaccess</artifactId>
    </dependency>
    <dependency>
        <groupId>org.egovframe.rte</groupId>
        <artifactId>egovframe-rte-fdl-idgnr</artifactId>
    </dependency>
    <dependency>
        <groupId>org.egovframe.rte</groupId>
        <artifactId>egovframe-rte-fdl-property</artifactId>
    </dependency>
    <!-- v5.0 신규 — @EgovNullCheck 등 Reactive Validation 어노테이션 사용 시 필수 -->
    <dependency>
        <groupId>org.egovframe.rte</groupId>
        <artifactId>egovframe-rte-ptl-reactive</artifactId>
    </dependency>
</dependencies>
```

> **artifactId 변경 표 (v4.3 → v5.0):**
>
> | v4.3 | v5.0 |
> |------|------|
> | `org.egovframe.rte.ptl.mvc` | `egovframe-rte-ptl-mvc` |
> | `org.egovframe.rte.psl.dataaccess` | `egovframe-rte-psl-dataaccess` |
> | `org.egovframe.rte.fdl.idgnr` | `egovframe-rte-fdl-idgnr` |
> | `org.egovframe.rte.fdl.property` | `egovframe-rte-fdl-property` |
> | (해당 없음) | `egovframe-rte-ptl-reactive` (신규) |

```xml
<!-- ❌ v4.3 artifactId 그대로 사용 — dependency resolution 실패 -->
<artifactId>org.egovframe.rte.ptl.mvc</artifactId>

<!-- ❌ <version> 명시 — Boot Parent 버전 관리 우회 (조합 충돌 위험) -->
<dependency>
    <groupId>org.egovframe.rte</groupId>
    <artifactId>egovframe-rte-ptl-mvc</artifactId>
    <version>5.0.0</version>  <!-- ❌ Parent 가 관리 -->
</dependency>

<!-- ❌ commons-logging exclusion 누락 — SLF4J 충돌 (RTE 의존성 추가 시 필요한 경우 적용) -->
```

---

### Spring Boot 호환성 매트릭스 (eGovFrame Boot Parent 5.0.0)

eGovFrame Boot Parent **5.0.0** 은 다음 환경을 고정한다 (Parent 가 자동 관리):

| 항목 | 버전 | 비고 |
|------|------|------|
| Java | **17** (표준프레임워크 5.0 공식 기준) | Boot 3.x 요구 (Java 21 도 동작은 하나 v5.0 표준은 17) |
| Spring Boot | **3.x** | jakarta.* 패키지 |
| Spring Framework | **6.x** | |
| Thymeleaf | **3.1.x** + spring6 dialect | `org.thymeleaf.spring6` |
| Jakarta EE | **9+** | `jakarta.*` 사용 |
| MyBatis Spring Boot Starter | **Parent 가 관리하지 않음 → `<version>3.0.3</version>` 명시 필요** | Spring Boot 3.x 호환은 mybatis-spring-boot-starter **3.0.x** |
| commons-lang3 | Parent 가 관리 (3.14+) | v4.3 의 `commons-lang3.version` 오버라이드 **불필요** |

> **v4.3 → v5.0 이행 시 제거할 설정:**
> - `<commons-lang3.version>3.14.0</commons-lang3.version>` ← 더 이상 필요 없음
> - `<org.egovframe.rte.version>4.3.0</org.egovframe.rte.version>` ← Parent 가 관리

> **반대로, v5.0 에서도 명시해야 하는 설정:**
> - `mybatis-spring-boot-starter` 의 `<version>3.0.3</version>` 은 **Boot Parent 5.0.0 이 관리하지 않으므로 반드시 명시**해야 함. 누락 시 `'dependencies.dependency.version' for org.mybatis.spring.boot:mybatis-spring-boot-starter:jar is missing` 오류로 빌드 실패.

```xml
<!-- ✅ v5.0 — mybatis-spring-boot-starter 는 명시적 버전 지정 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>  <!-- Spring Boot 3.x 호환 -->
</dependency>

<!-- ❌ v5.0 에서 version 생략 — Parent 가 관리하지 않아 빌드 실패 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <!-- ❌ <version> 누락 시 'version is missing' 오류 -->
</dependency>

<!-- ❌ v4.3 잔재 — 2.3.1 은 Spring Boot 2.7 용. Boot 3.x 와 호환 안 됨 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.1</version>  <!-- ❌ Boot 3.x 에서 NoClassDefFoundError 발생 -->
</dependency>
```

#### Java EE → Jakarta EE 패키지 마이그레이션

Spring Boot 3.x 는 **모든 `javax.*` 엔터프라이즈 API 가 `jakarta.*` 로 이동**. 단순 import 변경이 아닌 **모든 코드** 점검 필요.

```java
// ❌ v4.3 (Spring Boot 2.7) — javax.*
import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import javax.persistence.Entity;

// ✅ v5.0 (Spring Boot 3.x) — jakarta.*
import jakarta.annotation.Resource;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.Entity;

// ⚠️ javax.sql.DataSource 는 그대로 (Java SE 의 일부)
import javax.sql.DataSource;  // ✅ 변경 없음
```

#### Thymeleaf Spring Dialect 패키지

```java
// ❌ v4.3 — spring5
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

// ✅ v5.0 — spring6
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
```

#### Logging 스택 — Log4j2 필수 (v5.0 동일)

Spring Boot 기본 스타터는 **Logback**, eGovFrame 은 **Log4j2** 를 전제로 함.
동시 존재 시 `SLF4J: Class path contains multiple SLF4J bindings` 경고/충돌 발생.

```xml
<!-- ✅ Logback 은 반드시 exclusion, Log4j2 스타터 추가 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
<!-- v5.0 권장 — Bean Validation 표준 지원 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

```xml
<!-- ❌ starter-logging exclusion 누락 — Logback ↔ Log4j2 바인딩 충돌 -->
<!-- ❌ starter-log4j2 누락 — eGovFrame 내부 로깅 동작 불가 -->
<!-- ❌ starter-validation 누락 — @Valid / LocalValidatorFactoryBean 동작 불가 -->
```

---

**라이브러리 규칙:**
- `org.egovframe.rte.*` JAR 는 **수정 금지** — MD5/SHA1 이 원본 배포본과 일치해야 한다.
- Spring Framework / Spring Boot 버전을 transitive deps 로 임의 변경 금지(정당한 패치 업그레이드는 예외).
- v5.0 Boot Parent 가 관리하는 의존성 버전은 **`<properties>` 또는 `<version>` 으로 덮어쓰지 말 것** (조합 검증된 버전이 깨짐).
- `commons-logging` 충돌이 발생할 경우에 한해 RTE 의존성에 exclusion 처리.

**확장 규칙** — eGovFrame RTE 클래스를 확장할 때:

```java
// ❌ Forbidden 패키지
package org.egovframe.rte.custom; // ❌ rte 패키지 안에 클래스 정의 금지
public class MyDAO extends EgovAbstractDAO { }

// ❌ Forbidden 네이밍
public class EgovCustomDAO extends EgovAbstractDAO { } // ❌ "Egov" 로 시작 금지

// ✅ 올바른 확장
package com.example.common.dao;
public class CommonDAO extends EgovAbstractDAO { }
```

---

## Overall Application Rules

- 요청 흐름은 `Controller → Service → DAO` 를 유지한다. **계층 건너뛰기 금지.**
- 모든 프로젝트는 Controller / Service / DAO 세 계층을 모두 포함하고, Service 는 인터페이스/구현 페어로 둔다.

**패키지 레이아웃** — 둘 중 하나를 선택하고 **프로젝트 전체에 일관 적용**:

```
// ✅ 도메인 우선 (v5.0 공식 템플릿이 채택한 방식)
egovframework.example.sample.web.EgovSampleController
egovframework.example.sample.service.EgovSampleService
egovframework.example.sample.service.SampleVO
egovframework.example.sample.service.impl.EgovSampleServiceImpl
egovframework.example.sample.service.impl.SampleMapper

// ✅ 레이어 우선 (전체 일관 적용)
com.example.controller.sample.SampleController
com.example.service.sample.SampleService
com.example.dao.sample.SampleMapper

// ❌ 혼합 — 금지
com.example.sample.controller.SampleController   // 도메인 우선
com.example.service.other.OtherService           // 레이어 우선 (혼합)
```

**네이밍 컨벤션**

```java
// ✅ 단어 조합 스타일 (전체 일관 적용)
EgovSampleController, EgovSampleServiceImpl, SampleMapper, SampleVO
```
