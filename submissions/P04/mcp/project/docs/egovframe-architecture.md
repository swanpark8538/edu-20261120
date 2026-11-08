# eGovFrame Architecture Patterns (v5.0) — 개발자 레퍼런스

> **이 문서는** eGovFrame 5.x (Spring Boot 3.x / Spring 6.x / Java 17 / Jakarta EE) 프로젝트의 표준 아키텍처 패턴을 한곳에 모은 **개발자용 레퍼런스** 입니다. 신규 합류자 온보딩, 패턴 의도 확인, 예제 코드 참조용으로 펴 보세요.
>
> **AI 코딩 에이전트용 가이드는 별도로 제공됩니다:**
> - **빠른 룰북 / invariants** (Bean 체크리스트·Import 표·코딩 룰) → `egovframe-conventions` 스킬 (`.claude/skills/`, `.cursor/skills/` 등)
> - **에러 → 원인 → fix 카탈로그** → `egovframe-troubleshooting` 스킬
>
> **요지:** 각 패턴은 Controller → Service → DAO 3계층 흐름을 완전하게 보여줍니다. import 는 모두 `jakarta.*` 를 사용합니다.

## 목차

1. [Bootstrap Beans (기동 필수 Bean)](#bootstrap-beans-기동-필수-bean)
   - EgovConfigCommon — LeaveaTrace + MessageSource
   - EgovConfigProperties — EgovPropertyServiceImpl
   - EgovConfigIdGeneration — EgovTableIdGnrServiceImpl
   - EgovConfigDatasource — HSQL 임베디드
   - EgovConfigMapper — MyBatis SqlSessionFactory + @MapperScan
   - EgovConfigTransaction — 프로그래밍 방식 트랜잭션
   - EgovConfigAspect — 예외 전파 AOP
   - EgovConfigValidation — Bean Validation + 메시지 통합
   - EgovConfigWebSecurity — Spring Security 보안 설정 (보안 헤더·CSRF·StrictHttpFirewall·CORS)
   - EgovConfigWeb — 메인 Web Config (Import 결합)
   - 핵심 클래스 Fully-Qualified Import 표
2. [Pagination (페이징 처리) — KRDS 페이지네이션](#pagination-페이징-처리--krds-페이지네이션)
3. [application.properties (v5.0)](#applicationproperties-v50)

> 본 문서에 아직 포함되지 않은 주제 (File Upload, Authentication 등) 는 [eGovFrame 공식 문서](https://www.egovframe.go.kr) 를 참고하세요.

---

## Bootstrap Beans (기동 필수 Bean)

> **왜 필요한가:** `EgovAbstractServiceImpl` 또는 `EgovPropertyServiceImpl` 를 사용하는 Boot 프로젝트는
> 다음 Bean 들을 **반드시 `@Configuration` 클래스에 수동 등록**해야 한다.
> 표준 아키텍처 패턴(Controller/Service/Mapper)만 따르고 이 섹션을 건너뛰면 **기동 단계에서 실패**한다.
>
> 📋 **빠른 체크리스트만 보려면**: `egovframe-conventions` 스킬의 *Quick Reference → Bootstrap Bean 12종* 섹션. 본 문서는 각 `@Configuration` 클래스의 **상세 구현**을 다룬다.

### v5.0 권장 — 책임별 @Configuration 분할

v5.0 공식 템플릿은 모든 Bean 을 단일 클래스에 두지 않고 **9개 파일로 분할**한다 (`@Import` 로 결합). 각 파일은 단일 책임만 가진다.

```
egovframework.example.config/
├── EgovConfigCommon.java       # MessageSource, LeaveaTrace, AntPathMatcher 등
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

### 생성 필요 Bean 체크리스트

- [ ] `antPathMatcher` — 공용 `AntPathMatcher` Bean
- [ ] `defaultTraceHandler` — `LeaveaTrace` 의 핸들러
- [ ] `traceHandlerService` — `DefaultTraceHandleManager`
- [ ] `leaveaTrace` — `EgovAbstractServiceImpl` 필수 의존
- [ ] `messageSource` — 다중 basename (앱 메시지 + RTE 내장)
- [ ] `messageSourceAccessor` — `EgovPropertyServiceImpl` 등이 사용
- [ ] `propertiesService` — `pageUnit`/`pageSize` 등 전역 프로퍼티 (Map 또는 파일 기반)
- [ ] `egovIdGnrService` — PK 생성 (`EgovTableIdGnrServiceImpl`)
- [ ] `dataSource` — DataSource Bean
- [ ] `sqlSession` — `SqlSessionFactoryBean` (Bean name 주의: DAO 방식 필요시 `sqlSession`)
- [ ] `sqlSessionTemplate` — `@MapperScan` 인터페이스 매퍼용
- [ ] `txManager` / `txAdvice` / `txAdvisor` — Java Config 기반 트랜잭션
- [ ] Validator — `LocalValidatorFactoryBean` (messageSource 결합)
- [ ] `EgovKrdsPaginationRenderer` + `EgovPaginationDialect` — 페이징 UI

---

### EgovConfigCommon — LeaveaTrace + MessageSource

`EgovAbstractServiceImpl` 는 내부적으로 `@Resource(name="leaveaTrace")` 로 Bean 을 요구한다.
`EgovPropertyServiceImpl` 는 메시지 코드를 `MessageSource` 로 조회한다.

```java
// ✅ v5.0 표준 — Common Config
@Configuration
public class EgovConfigCommon {

    @Bean
    public AntPathMatcher antPathMatcher() {
        return new AntPathMatcher();
    }

    @Bean
    public DefaultTraceHandler defaultTraceHandler() {
        return new DefaultTraceHandler();
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        // v5.0 — 다중 basename. 앱 메시지 + RTE 의 idgnr / property 내장 메시지
        ms.setBasenames(
            "classpath:/egovframework/message/message-common",
            "classpath:/org/egovframe/rte/fdl/idgnr/messages/idgnr",
            "classpath:/org/egovframe/rte/fdl/property/messages/properties"
        );
        ms.setDefaultEncoding("UTF-8");
        ms.setCacheSeconds(60);
        return ms;
    }

    @Bean
    public MessageSourceAccessor messageSourceAccessor() {
        return new MessageSourceAccessor(messageSource());
    }

    @Bean
    public DefaultTraceHandleManager traceHandlerService() {
        DefaultTraceHandleManager mgr = new DefaultTraceHandleManager();
        mgr.setReqExpMatcher(antPathMatcher());
        mgr.setPatterns(new String[]{"*"});
        mgr.setHandlers(new TraceHandler[]{defaultTraceHandler()});
        return mgr;
    }

    @Bean
    public LeaveaTrace leaveaTrace() {
        LeaveaTrace bean = new LeaveaTrace();
        bean.setTraceHandlerServices(new TraceHandlerService[]{traceHandlerService()});
        return bean;
    }
}
```

```java
// ❌ leaveaTrace Bean 미등록 — 모든 Service 기동 실패
// ❌ messageSource basename 단일화 — RTE 내부 메시지 코드 (예: idgnr.message_xxx) 조회 실패
// ❌ MessageSourceAccessor Bean 누락 — RTE 일부 컴포넌트가 직접 의존
```

---

### EgovConfigProperties — EgovPropertyServiceImpl

v5.0 공식 템플릿은 **`Map<String,String>` 인라인 등록 방식**을 권장 (외부 properties 파일 방식보다 단순).

```java
// ✅ v5.0 — Map 기반 등록 (공식 템플릿 패턴)
@Configuration
public class EgovConfigProperties {

    @Bean(destroyMethod = "destroy")
    public EgovPropertyServiceImpl propertiesService() {
        Map<String, String> properties = new HashMap<>();
        properties.put("pageUnit", "10");
        properties.put("pageSize", "10");

        EgovPropertyServiceImpl bean = new EgovPropertyServiceImpl();
        bean.setProperties(properties);
        return bean;
    }
}
```

**파일 기반이 필요한 경우** (다수 properties 통합 관리 등):

```java
// ✅ classpath 문자열의 Set<String> — setExtFileName 시그니처는 Set<String>
@Bean(destroyMethod = "destroy")
public EgovPropertyServiceImpl propertiesService() {
    Set<String> files = new HashSet<>();
    files.add("classpath:egovframework/egovProps/globals.properties");

    EgovPropertyServiceImpl bean = new EgovPropertyServiceImpl();
    bean.setExtFileName(files);
    return bean;
}
```

```java
// ❌ Resource[] 전달 — 컴파일 에러 (incompatible types)
bean.setExtFileName(new Resource[]{new ClassPathResource("...")});

// ❌ Set<Resource> 전달 — 런타임 ClassCastException (내부에서 (String) 캐스팅)
Set<Resource> resources = new HashSet<>();
resources.add(new ClassPathResource("..."));
bean.setExtFileName(resources);

// ❌ destroyMethod 누락 — Bean 정리 시 리소스 핸들 leak 가능
@Bean // ← destroyMethod="destroy" 누락
public EgovPropertyServiceImpl propertiesService() { ... }
```

---

### EgovConfigIdGeneration — EgovTableIdGnrServiceImpl

> **v5.0 클래스명 변경 주의:**
> - v4.x: `EgovTableIdGnrService` (Impl 접미사 없음)
> - v5.0: **`EgovTableIdGnrServiceImpl`** (Impl 접미사 추가, 같은 `org.egovframe.rte.fdl.idgnr.impl` 패키지)
> - `afterPropertiesSet()` 수동 호출 불필요

```java
// ✅ v5.0 — 접두어 "SAMPLE-" + 5자리 숫자 (예: SAMPLE-00001)
@Configuration
public class EgovConfigIdGeneration {

    @Bean
    public EgovIdGnrStrategyImpl mixPrefixSample() {
        EgovIdGnrStrategyImpl strategy = new EgovIdGnrStrategyImpl();
        strategy.setPrefix("SAMPLE-");
        strategy.setCipers(5);
        strategy.setFillChar('0');
        return strategy;
    }

    @Bean(destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl egovIdGnrService(@Qualifier("dataSource") DataSource dataSource) {
        EgovTableIdGnrServiceImpl bean = new EgovTableIdGnrServiceImpl();
        bean.setDataSource(dataSource);
        bean.setStrategy(mixPrefixSample());
        bean.setBlockSize(10);
        bean.setTable("IDS");
        bean.setTableName("SAMPLE");
        return bean;  // afterPropertiesSet() 불필요 (Spring 라이프사이클이 처리)
    }
}
```

`IDS` 테이블 DDL (`db/sampledb.sql`):

```sql
CREATE MEMORY TABLE IDS (
    TABLE_NAME  VARCHAR(16)  NOT NULL PRIMARY KEY,
    NEXT_ID     DECIMAL(30)  NOT NULL
);
```

```java
// ❌ v4.x 클래스 그대로 사용 — v5.0 에서 클래스명 변경
import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrService; // ❌
EgovTableIdGnrService bean = new EgovTableIdGnrService();      // ❌

// ✅ v5.0 — Impl 접미사 추가
import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
EgovTableIdGnrServiceImpl bean = new EgovTableIdGnrServiceImpl();

// ❌ import 경로 오류 (매우 흔함) — Strategy 는 .impl.strategy 하위
import org.egovframe.rte.fdl.idgnr.impl.EgovIdGnrStrategyImpl;  // ❌ 존재하지 않음

// ✅ 올바른 import — .impl.strategy 하위 패키지
import org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl;
```

---

### EgovConfigDatasource — HSQL 임베디드 (개발/테스트)

```java
// ✅ v5.0 — EmbeddedDatabaseBuilder 로 HSQL 임베디드 DB
@Configuration
public class EgovConfigDatasource {

    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.HSQL)
            .addScript("classpath:/db/sampledb.sql")
            .build();
    }
}
```

운영 환경은 HikariCP / DBCP2 로 교체:

```java
// ✅ HikariCP (운영)
@Bean(name = "dataSource")
public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:postgresql://...");
    config.setUsername("...");
    config.setPassword("...");
    return new HikariDataSource(config);
}
```

---

### EgovConfigMapper — MyBatis SqlSessionFactory + @MapperScan

v5.0 권장 패턴은 **XML config + Java MapperScan 혼합**. typeAlias 와 settings 는 `sql-mapper-config.xml` 에, mapper 인터페이스 등록은 `@MapperScan` 으로 처리.

```java
// ✅ v5.0 표준 — Mapper Config
@Configuration
@MapperScan(basePackages = "egovframework.example.sample.service.impl")
public class EgovConfigMapper {

    // Bean name "sqlSession" — DAO 방식 사용 시 EgovAbstractMapper 가 찾는 Bean
    @Bean(name = "sqlSession")
    public SqlSessionFactoryBean sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource)
            throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setConfigLocation(resolver.getResource(
            "classpath:/egovframework/sqlmap/example/sql-mapper-config.xml"));
        bean.setMapperLocations(resolver.getResources(
            "classpath:/egovframework/sqlmap/example/mappers/*.xml"));
        return bean;
    }

    @Bean(name = "sqlSessionTemplate")
    public SqlSessionTemplate sqlSession(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
```

`sql-mapper-config.xml` — `egovMap` 별칭과 도메인 VO 별칭을 한 곳에서 정의:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <typeAliases>
        <typeAlias alias="egovMap" type="org.egovframe.rte.psl.dataaccess.util.EgovMap"/>
        <typeAlias alias="searchVO" type="egovframework.example.sample.service.SampleDefaultVO"/>
        <typeAlias alias="sampleVO" type="egovframework.example.sample.service.SampleVO"/>
    </typeAliases>
</configuration>
```

```java
// ❌ application.properties 에 mybatis.* 만 설정 — Java Config 와 충돌
// ❌ Bean name "sqlSession" 누락 — EgovAbstractMapper.setSqlSessionFactory 자동 주입 실패
// ❌ @MapperScan 누락 — @EgovMapper 인터페이스가 Bean 으로 등록되지 않음
```

---

### EgovConfigTransaction — 프로그래밍 방식 트랜잭션

v5.0 공식 템플릿은 `@EnableTransactionManagement` + `@Transactional` 대신 **AspectJ Pointcut 기반 자동 트랜잭션**을 권장 (네이밍 규칙으로 `*Impl` 클래스 전체를 한 번에 적용).

```java
// ✅ v5.0 — Java Config 트랜잭션 (모든 *ServiceImpl 자동 적용)
@Configuration
public class EgovConfigTransaction {

    @Bean(name = "txManager")
    public DataSourceTransactionManager txManager(@Qualifier("dataSource") DataSource dataSource) {
        DataSourceTransactionManager mgr = new DataSourceTransactionManager();
        mgr.setDataSource(dataSource);
        return mgr;
    }

    @Bean
    public TransactionInterceptor txAdvice(DataSourceTransactionManager txManager) {
        RuleBasedTransactionAttribute txAttribute = new RuleBasedTransactionAttribute();
        txAttribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        txAttribute.setRollbackRules(
            Collections.singletonList(new RollbackRuleAttribute(Exception.class)));

        HashMap<String, TransactionAttribute> txMethods = new HashMap<>();
        txMethods.put("*", txAttribute);

        NameMatchTransactionAttributeSource src = new NameMatchTransactionAttributeSource();
        src.setNameMap(txMethods);

        TransactionInterceptor txAdvice = new TransactionInterceptor();
        txAdvice.setTransactionAttributeSource(src);
        txAdvice.setTransactionManager(txManager);
        return txAdvice;
    }

    @Bean
    public Advisor txAdvisor(@Qualifier("txManager") DataSourceTransactionManager txManager) {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        // 도메인에 맞춰 패턴 조정 — 모든 sample..impl.*Impl
        pointcut.setExpression("execution(* egovframework.example.sample..impl.*Impl.*(..))");
        return new DefaultPointcutAdvisor(pointcut, txAdvice(txManager));
    }
}
```

```java
// ❌ Pointcut 표현식이 너무 좁음 — 새 도메인 추가 시 누락 위험
pointcut.setExpression("execution(* egovframework.example.sample.service.impl.EgovSampleServiceImpl.*(..))");
// ✅ 와일드카드로 모든 Impl 포괄
pointcut.setExpression("execution(* egovframework.example..impl.*Impl.*(..))");
```

---

### EgovConfigAspect — 예외 전파 AOP

v5.0 공식 템플릿은 Service 레이어 예외를 별도 핸들러로 전파하기 위해 `EgovAopExceptionTransfer` + `ExceptionTransfer` 조합을 사용.

> **import 경로 주의 (v5.0):**
> - `ExceptionHandler` → `org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler`
> - `ExceptionHandlerService` → `org.egovframe.rte.fdl.cmmn.exception.**manager**.ExceptionHandlerService` (`.handler` 가 아니라 **`.manager`** 하위)
> - `DefaultExceptionHandleManager` → `org.egovframe.rte.fdl.cmmn.exception.manager.DefaultExceptionHandleManager`
>
> `ExceptionHandlerService` 를 `.handler` 패키지에서 찾으면 **`cannot find symbol: class ExceptionHandlerService`** 컴파일 오류 발생.

```java
// ✅ v5.0 — Aspect Config (예외 전파)
import org.egovframe.rte.fdl.cmmn.aspect.ExceptionTransfer;
import org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler;
import org.egovframe.rte.fdl.cmmn.exception.manager.DefaultExceptionHandleManager;
import org.egovframe.rte.fdl.cmmn.exception.manager.ExceptionHandlerService;  // ★ .manager 하위

@Configuration
@EnableAspectJAutoProxy
public class EgovConfigAspect {

    @Bean
    public EgovSampleExcepHndlr egovHandler() {
        return new EgovSampleExcepHndlr();
    }

    @Bean
    public DefaultExceptionHandleManager defaultExceptionHandleManager(
            AntPathMatcher antPathMatcher, EgovSampleExcepHndlr egovHandler) {
        DefaultExceptionHandleManager mgr = new DefaultExceptionHandleManager();
        mgr.setReqExpMatcher(antPathMatcher);
        mgr.setPatterns(new String[]{"**service.impl.*"});
        mgr.setHandlers(new ExceptionHandler[]{egovHandler});
        return mgr;
    }

    @Bean
    public ExceptionTransfer exceptionTransfer(
            DefaultExceptionHandleManager defaultExceptionHandleManager) {
        ExceptionTransfer transfer = new ExceptionTransfer();
        transfer.setExceptionHandlerService(
            new ExceptionHandlerService[]{defaultExceptionHandleManager});
        return transfer;
    }

    @Bean
    public EgovAopExceptionTransfer aopExceptionTransfer(ExceptionTransfer exceptionTransfer) {
        EgovAopExceptionTransfer aop = new EgovAopExceptionTransfer();
        aop.setExceptionTransfer(exceptionTransfer);
        return aop;
    }
}
```

핸들러 구현:

```java
@Slf4j
public class EgovSampleExcepHndlr implements ExceptionHandler {
    @Override
    public void occur(Exception ex, String packageName) {
        log.debug("##### EgovSampleExcepHndlr Run...");
    }
}
```

`EgovAopExceptionTransfer` (예외를 가로채 핸들러로 위임):

```java
@Aspect
public class EgovAopExceptionTransfer {

    private ExceptionTransfer exceptionTransfer;

    public void setExceptionTransfer(ExceptionTransfer exceptionTransfer) {
        this.exceptionTransfer = exceptionTransfer;
    }

    @Pointcut("execution(* egovframework.example..impl.*Impl.*(..))")
    private void exceptionTransferService() {}

    @AfterThrowing(pointcut = "exceptionTransferService()", throwing = "ex")
    public void doAfterThrowingExceptionTransferService(JoinPoint jp, Exception ex) throws Exception {
        exceptionTransfer.transfer(jp, ex);
    }
}
```

---

### EgovConfigValidation — Bean Validation + 메시지 통합

`@EgovNullCheck`, `@NotNull` 등 검증 어노테이션의 메시지를 `messageSource` 와 연결.

```java
// ✅ v5.0 — Validator Config
@Configuration
public class EgovConfigValidation {

    @Bean
    public Validator getValidator(@Qualifier("messageSource") MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
```

VO 검증 어노테이션 — v5.0 신규 `@EgovNullCheck` (Reactive 모듈):

```java
import org.egovframe.rte.ptl.reactive.validation.EgovNullCheck;

public class SampleVO extends SampleDefaultVO {
    private String id;

    @EgovNullCheck(message = "{confirm.required.name}")  // messageSource 코드 참조
    private String name;

    @EgovNullCheck(message = "{confirm.required.description}")
    private String description;
}
```

> **`@EgovNullCheck` 사용 조건:** `egovframe-rte-ptl-reactive` 의존성이 pom.xml 에 있어야 한다.

> **⚠️ 절대 규칙 — `@EgovNullCheck` 는 `String` 필드 전용:**
> 이 어노테이션의 ConstraintValidator 는 내부적으로 `String` 타입만 처리하도록 구현되어 있다.
> `Timestamp`, `Date`, `LocalDate`, `LocalDateTime`, `Long`, `Integer`, `BigInteger`,
> `BigDecimal`, `Boolean`, enum, 컬렉션, 사용자 정의 객체 등 **비-String 타입 어떤 것에도
> 절대 붙이지 말 것**. 붙이면 Bean 초기화 시점에 `HV000030: No validator could be found
> for constraint ... validating type 'java.sql.Timestamp'` 가 발생해 컨텍스트 로딩이 실패한다.
>
> ```java
> // ❌ 절대 금지 — 기동 시점에 HV000030 으로 실패
> @EgovNullCheck(message = "{confirm.required.createdAt}")
> private Timestamp createdAt;          // ❌ 비-String
>
> @EgovNullCheck(message = "{confirm.required.deptId}")
> private BigInteger deptId;            // ❌ 비-String
>
> @EgovNullCheck(message = "{confirm.required.useYn}")
> private Boolean useYn;                // ❌ 비-String
>
> // ✅ 비-String 필드의 필수 검증은 jakarta.validation 표준 어노테이션 사용:
> @NotNull(message = "{confirm.required.createdAt}")
> private Timestamp createdAt;          // ✅
>
> @NotNull(message = "{confirm.required.deptId}")
> @Min(value = 0, message = "{validation.deptId.min}")
> private BigInteger deptId;            // ✅
> ```
>
> `egov_scaffold_project` / `egov_add_entity` 가 만든 VO 는 분기 로직으로 String 필드에만
> `@EgovNullCheck` 를 붙여 생성한다. **AI 에이전트가 사후에 VO 를 편집·확장할 때 비-String
> 필드에 같은 어노테이션을 잘못 추가하지 않도록 반드시 주의할 것.** 필요한 경우 위 예시처럼
> `@NotNull` (+ 도메인에 맞는 추가 제약) 으로 대체한다.

---

### EgovConfigWebSecurity — 보안 헤더·CSRF·StrictHttpFirewall·CORS

공통컴포넌트(security 방식)의 `egov-security-config.properties` +
`EgovSecurityConfiguration`(egovframe-rte-fdl-security) 구조를 참조한
Spring Security 설정. 값은 `application.properties` 의 `egov.security.*` 로 주입된다.

| 프로퍼티 그룹 | 기본값 | 내용 |
|--------------|--------|------|
| `egov.security.headers.*` | sniff=true, xframe-options=SAMEORIGIN, xss-protection=true, cache-control=true | 보안 응답 헤더 (공통컴포넌트 속성명과 동일 의미) |
| `egov.security.csrf` / `csrf-access-denied-url` | true / `/egovCSRFAccessDenied.do` | CSRF 보호 + 검증 실패 안내 화면 포워드 (`EgovCsrfControllerAdvice` 가 `${_csrf}` 노출) |
| `egov.security.cors.*` | enabled=false | 교차 출처 화이트리스트 (기본 비활성 = 공통컴포넌트와 동일한 차단 자세) |

`StrictHttpFirewall` 과 400 거부 핸들러는 Spring Security 기본값을 명시적
빈으로 드러낸 것으로 동작 변화는 없다. 인증 미도입 상태라 필터 체인은
`anyRequest().permitAll()` 이며, 인증 도입 시 `requestMatchers(...)` 로 보호
URL 을 좁힌다. 전체 구현은 생성된 `config/EgovConfigWebSecurity.java` 참조.

---

### EgovConfigWeb — 메인 Web Config (Import 결합)

```java
// ✅ v5.0 — 메인 Web Config: 다른 설정을 @Import 로 끌어옴
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
public class EgovConfigWeb implements WebMvcConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("classpath:/templates/thymeleaf/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(true);
        return resolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine(EgovKrdsPaginationRenderer renderer) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver());
        engine.setEnableSpringELCompiler(true);
        engine.addDialect(new EgovPaginationDialect(renderer)); // 페이징 Dialect 등록
        return engine;
    }

    @Bean
    public ThymeleafViewResolver thymeleafViewResolver(EgovKrdsPaginationRenderer renderer) {
        ThymeleafViewResolver vr = new ThymeleafViewResolver();
        vr.setCharacterEncoding("UTF-8");
        vr.setTemplateEngine(templateEngine(renderer));
        return vr;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/img/**").addResourceLocations("classpath:/static/img/");
    }
}
```

> **Thymeleaf 패키지 주의 (v5.0):**
> - `org.thymeleaf.spring6.SpringTemplateEngine` (NOT `spring5`)
> - `org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver`
> - `org.thymeleaf.spring6.view.ThymeleafViewResolver`

---

### 핵심 클래스 Fully-Qualified Import 표 (v5.0)

다음 클래스들은 v4.x 와 패키지/이름이 다르거나 자주 헷갈리는 경로이므로 **정확한 전체 경로** 를 외워두자.

> 📋 **AI 에이전트용 동일 표** (description-trigger 스킬): `egovframe-conventions` 스킬의 *Quick Reference → 핵심 클래스 Fully-Qualified Import* 섹션에 같은 내용이 있다.

| 클래스 | 전체 경로 | v4.3 와의 차이 |
|--------|-----------|----------------|
| `EgovAbstractServiceImpl` | `org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl` | 동일 |
| `LeaveaTrace` | `org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace` | 동일 |
| `TraceHandler` | `org.egovframe.rte.fdl.cmmn.trace.handler.TraceHandler` | 동일 |
| `DefaultTraceHandler` | `org.egovframe.rte.fdl.cmmn.trace.handler.DefaultTraceHandler` | 동일 |
| `TraceHandlerService` | `org.egovframe.rte.fdl.cmmn.trace.manager.TraceHandlerService` | 동일 |
| `DefaultTraceHandleManager` | `org.egovframe.rte.fdl.cmmn.trace.manager.DefaultTraceHandleManager` | 동일 |
| `EgovPropertyServiceImpl` | `org.egovframe.rte.fdl.property.impl.EgovPropertyServiceImpl` | 동일 |
| `EgovIdGnrService` | `org.egovframe.rte.fdl.idgnr.EgovIdGnrService` | 동일 |
| **`EgovTableIdGnrServiceImpl`** | `org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl` | ⚠️ **v4.x 는 `EgovTableIdGnrService`** |
| `EgovIdGnrStrategyImpl` | `org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl` | ⚠️ `.strategy` 하위 |
| `EgovMap` | `org.egovframe.rte.psl.dataaccess.util.EgovMap` | 동일 |
| **`@EgovMapper`** | `org.egovframe.rte.psl.dataaccess.mapper.EgovMapper` | ⚠️ **v4.x 는 `Mapper`** |
| `PaginationInfo` | `org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo` | 동일 |
| **`AbstractKrdsPaginationRenderer`** | `org.egovframe.rte.ptl.mvc.tags.ui.pagination.AbstractKrdsPaginationRenderer` | ⚠️ **v5.0 신규** |
| `ExceptionTransfer` | `org.egovframe.rte.fdl.cmmn.aspect.ExceptionTransfer` | 동일 |
| `ExceptionHandler` | `org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler` | 동일 (`handler` 하위) |
| **`ExceptionHandlerService`** | `org.egovframe.rte.fdl.cmmn.exception.**manager**.ExceptionHandlerService` | ⚠️ **`.handler` 아님 — `.manager` 하위** (자주 헷갈림) |
| `DefaultExceptionHandleManager` | `org.egovframe.rte.fdl.cmmn.exception.manager.DefaultExceptionHandleManager` | 동일 (`manager` 하위) |
| `EgovNullCheck` | `org.egovframe.rte.ptl.reactive.validation.EgovNullCheck` | ⚠️ **v5.0 신규** |
| `@Resource` | `jakarta.annotation.Resource` | ⚠️ **v4.x 는 `javax.*`** |
| `@PostConstruct` | `jakarta.annotation.PostConstruct` | ⚠️ **v4.x 는 `javax.*`** |
| `@Valid` | `jakarta.validation.Valid` | ⚠️ **v4.x 는 `javax.*`** |
| `SpringTemplateEngine` | `org.thymeleaf.spring6.SpringTemplateEngine` | ⚠️ **v4.x 는 `spring5`** |

---

## Pagination (페이징 처리) — KRDS 페이지네이션

v5.0 부터 **KRDS (Korea Government Design System) 기반의 페이지네이션 렌더러**가 표준이다.
4.x 의 `EgovPaginationFormat` (MessageFormat 기반) 대신 `AbstractKrdsPaginationRenderer` 를 상속한 `EgovKrdsPaginationRenderer` 를 사용한다.

### 데이터 흐름

```
Request(pageIndex)
    → Controller (PaginationInfo 설정, firstIndex/lastIndex 계산)
    → Service (목록 조회 + 총건수 조회 위임)
    → Mapper (SQL LIMIT/OFFSET)
    → Controller (totalRecordCount 설정)
    → View (#egovKrdsPaginationRenderer.renderPagination 으로 페이징 UI 렌더링)
```

### 생성 필요 파일 체크리스트

- [ ] `SampleDefaultVO.java` — 페이징 공통 필드
- [ ] `SampleVO.java` — 도메인 VO (DefaultVO 상속)
- [ ] `EgovSampleController.java` — PaginationInfo 설정
- [ ] `EgovSampleService.java` / `EgovSampleServiceImpl.java` — 목록 + 총건수
- [ ] `SampleMapper.java` (`@EgovMapper`) + `EgovSample_Sample_SQL.xml` — LIMIT/OFFSET SQL
- [ ] `EgovKrdsPaginationRenderer.java` — KRDS 렌더러 (`AbstractKrdsPaginationRenderer` 상속)
- [ ] `EgovPaginationDialect.java` — Thymeleaf Dialect
- [ ] `egovSampleList.html` — 페이징 UI 렌더링

---

### VO Layer — DefaultVO 상속 패턴

페이징 필드는 `SampleDefaultVO`에 정의하고, 도메인 VO가 이를 상속한다.

```java
// ✅ 페이징 공통 VO — 모든 목록 VO의 부모
public class SampleDefaultVO implements Serializable {
    private static final long serialVersionUID = -858838578081269359L;

    private String searchCondition = "";
    private String searchKeyword = "";
    private String searchUseYn = "";
    private int pageIndex = 1;           // 현재 페이지 (1-based)
    private int pageUnit = 10;           // 페이지당 레코드 수
    private int pageSize = 10;           // 페이지 링크 수
    private int firstIndex = 1;          // SQL OFFSET 값
    private int lastIndex = 1;           // SQL 마지막 인덱스
    private int recordCountPerPage = 10; // 페이지당 표시 건수

    // ... getter/setter
}
```

```java
// ✅ 도메인 VO — DefaultVO 상속 + jakarta.* + EgovNullCheck
public class SampleVO extends SampleDefaultVO {
    private static final long serialVersionUID = 1L;

    private String id;

    @EgovNullCheck(message = "{confirm.required.name}")
    private String name;

    @EgovNullCheck(message = "{confirm.required.description}")
    private String description;

    private String useYn;

    @EgovNullCheck(message = "{confirm.required.user}")
    private String regUser;
    // ... getter/setter
}
```

```java
// ❌ 페이징 필드를 SampleVO 에 직접 선언 — DefaultVO 상속 누락
public class SampleVO {
    private int pageIndex;  // ❌ DefaultVO 로 분리하라
    private String name;
}

// ❌ pageIndex 기본값 0 — eGovFrame 은 1-based (기본값 = 1)
private int pageIndex = 0; // ❌ 1 이어야 한다

// ❌ javax.validation 어노테이션 — Boot 3 에서 jakarta.* 사용
import javax.validation.constraints.NotNull; // ❌
```

---

### Controller Layer — PaginationInfo 설정

Controller에서 PaginationInfo를 생성하고, 인덱스를 계산하여 VO에 주입한다.

```java
// ✅ v5.0 — 페이징 목록 조회 (jakarta.* import)
@Controller
@Slf4j
public class EgovSampleController {

    @Resource(name = "sampleService")  // jakarta.annotation.Resource
    private EgovSampleService sampleService;

    @Resource(name = "propertiesService")
    private EgovPropertyService propertiesService;

    @GetMapping("/egovSampleList.do")
    public String selectSampleList(@ModelAttribute("sampleVO") SampleVO sampleVO,
                                    ModelMap model) throws Exception {

        // ① pageUnit, pageSize 를 프로퍼티에서 조회
        sampleVO.setPageUnit(propertiesService.getInt("pageUnit"));
        sampleVO.setPageSize(propertiesService.getInt("pageSize"));

        // ② PaginationInfo 생성 및 설정
        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(sampleVO.getPageIndex());
        paginationInfo.setRecordCountPerPage(sampleVO.getPageUnit());
        paginationInfo.setPageSize(sampleVO.getPageSize());

        // ③ 계산된 인덱스를 VO 에 주입 (SQL 에서 사용)
        sampleVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        sampleVO.setLastIndex(paginationInfo.getLastRecordIndex());
        sampleVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

        // ④ 목록 조회
        List<?> sampleList = sampleService.selectSampleList(sampleVO);
        model.addAttribute("resultList", sampleList);

        // ⑤ 총건수 조회 후 PaginationInfo 에 설정 (반드시 목록 조회 후)
        int totCnt = sampleService.selectSampleListTotCnt(sampleVO);
        paginationInfo.setTotalRecordCount(totCnt);
        model.addAttribute("paginationInfo", paginationInfo);

        return "sample/egovSampleList";
    }

    @PostMapping("/addSample.do")
    public String addSample(@Valid @ModelAttribute("sampleVO") SampleVO sampleVO,
                            BindingResult bindingResult,
                            Model model, SessionStatus status) throws Exception {

        if (bindingResult.hasErrors()) {
            model.addAttribute("sampleVO", sampleVO);
            return "sample/egovSampleRegister";
        }

        sampleService.insertSample(sampleVO);
        status.setComplete();
        return "redirect:/egovSampleList.do";
    }
}
```

```java
// ❌ pageUnit 을 하드코딩 — EgovPropertyService 에서 조회하라
sampleVO.setPageUnit(10); // ❌ propertiesService.getInt("pageUnit") 사용

// ❌ totalRecordCount 를 목록 조회 전에 설정 — firstIndex/lastIndex 계산 오류
paginationInfo.setTotalRecordCount(sampleService.selectSampleListTotCnt(sampleVO));
sampleService.selectSampleList(sampleVO); // ❌ 순서 뒤바뀜

// ❌ PaginationInfo 를 Service 에서 생성 — Controller 책임
sampleService.getPaginatedList(sampleVO); // ❌ Service 가 PaginationInfo 를 알면 안 된다
```

---

### Service Layer — 순수 위임

Service는 목록과 총건수를 각각 Mapper에 위임한다. PaginationInfo를 직접 다루지 않는다.

```java
// ✅ Service 인터페이스 — 목록 + 총건수 + CRUD
public interface EgovSampleService {
    void insertSample(SampleVO vo) throws Exception;
    void updateSample(SampleVO vo) throws Exception;
    void deleteSample(SampleVO vo) throws Exception;
    SampleVO selectSample(SampleVO vo) throws Exception;
    List<?> selectSampleList(SampleVO vo) throws Exception;
    int selectSampleListTotCnt(SampleVO vo);
}
```

```java
// ✅ Service 구현 — EgovAbstractServiceImpl 상속 + jakarta.* + 순수 위임
@Service("sampleService")
public class EgovSampleServiceImpl extends EgovAbstractServiceImpl implements EgovSampleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EgovSampleServiceImpl.class);

    @Resource(name = "sampleMapper")          // jakarta.annotation.Resource
    private SampleMapper sampleMapper;

    @Resource(name = "egovIdGnrService")
    private EgovIdGnrService egovIdGnrService;

    @Override
    public void insertSample(SampleVO vo) throws Exception {
        String id = egovIdGnrService.getNextStringId();  // PK 자동 채번
        vo.setId(id);
        sampleMapper.insertSample(vo);
    }

    @Override
    public SampleVO selectSample(SampleVO vo) throws Exception {
        SampleVO result = sampleMapper.selectSample(vo);
        if (result == null) {
            throw processException("info.nodata.msg");  // 부모 헬퍼
        }
        return result;
    }

    @Override
    public List<?> selectSampleList(SampleVO vo) throws Exception {
        return sampleMapper.selectSampleList(vo);
    }

    @Override
    public int selectSampleListTotCnt(SampleVO vo) {
        return sampleMapper.selectSampleListTotCnt(vo);
    }
}
```

```java
// ❌ Service 에서 PaginationInfo 생성 — Controller 책임 침범
@Override
public Map<String, Object> getPaginatedList(SampleVO vo) {
    PaginationInfo paginationInfo = new PaginationInfo(); // ❌ Controller 에서 하라
}

// ❌ throw new EgovBizException(...) 직접 사용 — processException 헬퍼 활용
throw new EgovBizException("info.nodata.msg"); // ❌
throw processException("info.nodata.msg");     // ✅ messageSource 와 자동 연결
```

---

### DAO/Mapper Layer — @EgovMapper + LIMIT/OFFSET

Mapper 인터페이스와 SQL XML에서 페이징을 처리한다. **목록 SQL과 카운트 SQL의 WHERE 조건이 반드시 일치**해야 한다.

```java
// ✅ v5.0 — @EgovMapper 어노테이션 (NOT @Mapper)
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

@EgovMapper("sampleMapper")
public interface SampleMapper {
    void insertSample(SampleVO vo) throws Exception;
    void updateSample(SampleVO vo) throws Exception;
    void deleteSample(SampleVO vo) throws Exception;
    SampleVO selectSample(SampleVO vo) throws Exception;
    List<?> selectSampleList(SampleVO vo) throws Exception;
    int selectSampleListTotCnt(SampleVO vo);
}
```

매퍼 XML — `classpath:/egovframework/sqlmap/example/mappers/EgovSample_Sample_SQL.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="egovframework.example.sample.service.impl.SampleMapper">

    <resultMap id="sample" type="egovframework.example.sample.service.SampleVO">
        <result property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="description" column="description"/>
        <result property="useYn" column="use_yn"/>
        <result property="regUser" column="reg_user"/>
    </resultMap>

    <!-- ✅ 목록 조회 — LIMIT/OFFSET 으로 페이징 -->
    <select id="selectSampleList" parameterType="searchVO" resultType="egovMap">
        SELECT ID, NAME, DESCRIPTION, USE_YN, REG_USER
        FROM SAMPLE
        WHERE 1=1
        <if test="searchKeyword != null and searchKeyword != ''">
            <choose>
                <when test="searchCondition == 0">
                    AND ID LIKE '%' || #{searchKeyword} || '%'
                </when>
                <when test="searchCondition == 1">
                    AND NAME LIKE '%' || #{searchKeyword} || '%'
                </when>
            </choose>
        </if>
        ORDER BY ID DESC
        LIMIT #{recordCountPerPage} OFFSET #{firstIndex}
    </select>

    <!-- ✅ 총건수 조회 — 목록과 동일한 WHERE 조건 -->
    <select id="selectSampleListTotCnt" parameterType="searchVO" resultType="int">
        SELECT COUNT(*) totcnt
        FROM SAMPLE
        WHERE 1=1
        <if test="searchKeyword != null and searchKeyword != ''">
            <choose>
                <when test="searchCondition == 0">
                    AND ID LIKE '%' || #{searchKeyword} || '%'
                </when>
                <when test="searchCondition == 1">
                    AND NAME LIKE '%' || #{searchKeyword} || '%'
                </when>
            </choose>
        </if>
    </select>
</mapper>
```

```java
// ❌ v4.x @Mapper 사용 — v5.0 에서는 @EgovMapper
import org.egovframe.rte.psl.dataaccess.mapper.Mapper;
@Mapper("sampleMapper") // ❌
```

```xml
<!-- ❌ 카운트 SQL 에 검색 조건 누락 — 페이징 총건수 불일치 -->
<select id="selectSampleListTotCnt" resultType="int">
    SELECT COUNT(*) FROM SAMPLE   <!-- ❌ WHERE 절 없음 -->
</select>

<!-- ❌ Java 코드에서 subList 로 페이징 — DB 레벨 LIMIT/OFFSET 사용하라 -->
```

---

### KRDS Pagination Renderer (v5.0 신규)

v5.0 은 `AbstractKrdsPaginationRenderer` 를 상속한 `EgovKrdsPaginationRenderer` 를 `@Component` 로 등록한다. 라벨은 `MessageSource` + Locale 기반으로 동적 생성.

```java
// ✅ v5.0 — KRDS 페이지네이션 렌더러
@Component
public class EgovKrdsPaginationRenderer extends AbstractKrdsPaginationRenderer {

    @Autowired
    private MessageSource messageSource;

    @PostConstruct  // jakarta.annotation.PostConstruct
    public void initMessageLabels() {
        initVariables();  // 기본값 초기화
        updateLabelsWithCurrentLocale();
    }

    public void initVariables() {
        previousPageLabel = "<a onclick=\"{0}({1})\" class=\"page-navi prev\" href=\"#\">이전</a>";
        previousPageDisabledLabel = "<span class=\"page-navi prev disabled\">이전</span>";
        firstPageLabel = "<a onclick=\"{0}({1})\" class=\"page-link\" href=\"#\">{2}</a>";
        currentPageLabel = "<a class=\"page-link active\" href=\"#\">{0}</a>";
        otherPageLabel = "<a onclick=\"{0}({1})\" class=\"page-link\" href=\"#\">{2}</a>";
        nextPageLabel = "<a onclick=\"{0}({1})\" class=\"page-navi next\" href=\"#\">다음</a>";
        nextPageDisabledLabel = "<span class=\"page-navi next disabled\">다음</span>";
        lastPageLabel = "<a onclick=\"{0}({1})\" class=\"page-link\" href=\"#\">{2}</a>";
        dotPageLabel = "<span class=\"page-link link-dot\">...</span>";
    }

    @Override
    public String renderPagination(PaginationInfo paginationInfo, String jsFunction) {
        updateLabelsWithCurrentLocale();  // 매 호출마다 Locale 반영
        return super.renderPagination(paginationInfo, jsFunction);
    }

    private void updateLabelsWithCurrentLocale() {
        String prevLabel = messageSource.getMessage("page.prev", null, "이전",
            LocaleContextHolder.getLocale());
        String nextLabel = messageSource.getMessage("page.next", null, "다음",
            LocaleContextHolder.getLocale());

        previousPageLabel = "<a onclick=\"{0}({1})\" class=\"page-navi prev\" href=\"#\">"
            + prevLabel + "</a>";
        previousPageDisabledLabel = "<span class=\"page-navi prev disabled\">"
            + prevLabel + "</span>";
        nextPageLabel = "<a onclick=\"{0}({1})\" class=\"page-navi next\" href=\"#\">"
            + nextLabel + "</a>";
        nextPageDisabledLabel = "<span class=\"page-navi next disabled\">"
            + nextLabel + "</span>";
    }
}
```

Thymeleaf Dialect — 표현식 객체로 렌더러를 노출:

```java
// ✅ v5.0 — Dialect 가 KrdsRenderer 를 직접 expression object 로 노출
public class EgovPaginationDialect extends AbstractDialect implements IExpressionObjectDialect {

    private final EgovKrdsPaginationRenderer renderer;

    public EgovPaginationDialect(EgovKrdsPaginationRenderer renderer) {
        super("EgovPaginationDialect");
        this.renderer = renderer;
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {
            @Override
            public Set<String> getAllExpressionObjectNames() {
                // ⚠️ v4.x 의 "egovPaginationFormat" 이 아니라 "egovKrdsPaginationRenderer"
                return Collections.singleton("egovKrdsPaginationRenderer");
            }

            @Override
            public Object buildObject(IExpressionContext context, String expressionObjectName) {
                return renderer;
            }

            @Override
            public boolean isCacheable(String expressionObjectName) {
                return true;
            }
        };
    }
}
```

```java
// ❌ v4.x EgovPaginationFormat (MessageFormat 기반) 그대로 사용 — v5.0 에서는 KrdsRenderer 사용
// ❌ Dialect 미등록 — #egovKrdsPaginationRenderer 표현식 인식 불가
// ❌ engine.addDialect() 호출 누락 시 런타임 에러 발생
```

---

### View Layer — Thymeleaf 템플릿

```html
<!-- ✅ v5.0 — KrdsRenderer 호출 (메서드명: renderPagination) -->
<div id="paging" class="krds-pagination w-page"
     th:utext="${#egovKrdsPaginationRenderer.renderPagination(paginationInfo, 'samplePagination')}">
</div>

<!-- ✅ JavaScript 페이징 함수 -->
<script>
function samplePagination(pageIndex) {
    $('#pageIndex').val(pageIndex);
    $('#listForm').attr('method', 'get').attr('action', contextPath + '/egovSampleList.do').submit();
}
</script>

<!-- ✅ 숨겨진 form 필드 -->
<form id="listForm" th:object="${sampleVO}">
    <input type="hidden" id="pageIndex" name="pageIndex" th:value="1"/>
    <input type="hidden" name="searchCondition" th:value="${sampleVO.searchCondition}"/>
    <input type="hidden" name="searchKeyword" th:value="${sampleVO.searchKeyword}"/>
</form>
```

```html
<!-- ❌ v4.x 표현식 그대로 사용 — v5.0 에서는 객체명·메서드명이 다름 -->
<span th:utext="${#egovPaginationFormat.paginationFormat(paginationInfo, 'sample')}"></span>
<!-- ✅ v5.0 -->
<span th:utext="${#egovKrdsPaginationRenderer.renderPagination(paginationInfo, 'sample')}"></span>

<!-- ❌ th:text 사용 — HTML 이스케이프되어 태그가 문자열로 출력 -->
<span th:text="${#egovKrdsPaginationRenderer.renderPagination(paginationInfo, 'sample')}"></span>
<!-- ❌ 결과: &lt;a onclick=...&gt; 가 화면에 노출. th:utext 사용 -->
```

---

### 행 번호 계산 (목록 테이블)

```html
<!-- ✅ 역순 행 번호: (총건수+1) - ((페이지-1)*페이지크기 + 순번) -->
<td class="text-center"
    th:text="${paginationInfo.totalRecordCount + 1
        - ((sampleVO.pageIndex - 1) * sampleVO.pageSize + iterStat.count)}">
</td>
```

---

## application.properties (v5.0)

v5.0 공식 템플릿은 `application.properties` 형식을 사용하며, MyBatis 설정은 모두 Java Config 에서 처리하므로 properties 에는 두지 않는다.

```properties
server.port=8080

# 수동 Bean 이 자동 Bean 을 오버라이딩 하도록 허용
spring.main.allow-bean-definition-overriding=true

# JPA 영속성 컨텍스트를 트랜잭션 종료 시 같이 종료
spring.jpa.open-in-view=false
```

```properties
# ❌ mybatis.mapper-locations / type-aliases-package 등 — Java Config 와 충돌
mybatis.mapper-locations=classpath:egovframework/mapper/*.xml  # ❌ EgovConfigMapper 가 처리
```
