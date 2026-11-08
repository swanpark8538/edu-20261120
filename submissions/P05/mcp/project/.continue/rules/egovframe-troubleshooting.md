---
name: egovframe-troubleshooting
description: "전자정부 표준프레임워크(eGovFrame) 5.x Boot 프로젝트(Spring Boot 3.x / Java 17 / Jakarta EE)에서 컴파일 오류·기동 실패·Bean 초기화 예외가 발생했을 때 적용. NoClassDefFoundError, NoSuchBeanDefinitionException, ClassCastException, NoSuchMethodError, \"Failed to parse mapping resource\", \"cannot find symbol\", \"package javax.* does not exist\", \"release version 21 not supported\" 등 오류 메시지 진단 시 트리거. 신규 프로젝트 생성 직후 첫 `mvn spring-boot:run` 전 사전 점검용으로도 사용."
alwaysApply: false
---

# eGovFrame Troubleshooting (v5.0 + Spring Boot 3.x)

> **언제 적용:** eGovFrame 5.x Boot 프로젝트에서 **컴파일 오류·기동 실패·Bean 초기화 예외**가 발생했을 때. `egov_scaffold_project` 로 새 프로젝트를 생성한 직후 **첫 `mvn spring-boot:run` 전 사전 점검**용으로도 사용.
> **관계:** `egovframe-conventions` Skill 은 "코딩 invariants" (위반 시 본 카탈로그의 증상이 발생), 본 Skill 은 **그 증상의 실제 증상 → 원인 → fix** 대응표. 패턴·예제 코드는 별도 레퍼런스 문서 `docs/egovframe-architecture.md` 를 참고.
> **트리거 신호:** `NoClassDefFoundError`, `NoSuchBeanDefinitionException`, `ClassCastException`, `NoSuchMethodError`, `Failed to parse mapping resource`, `cannot find symbol`, `package javax.* does not exist`, `not applicable to class`(어노테이션) 등.

---

## 1. 컴파일 단계

### 1.1 `package javax.annotation does not exist` (또는 javax.validation, javax.servlet)

- **Symptom**
  ```
  [ERROR] EgovSampleServiceImpl.java:[8,20] package javax.annotation does not exist
  [ERROR] EgovSampleController.java:[12,20] package javax.validation does not exist
  ```
- **Cause**: Spring Boot 3.x (= eGovFrame 5.0) 는 **Jakarta EE 9+** 로 이전.
  `javax.*` 엔터프라이즈 API 가 모두 `jakarta.*` 로 이동.
- **Fix**: import 를 `jakarta.*` 로 일괄 교체.
  ```java
  // ❌ Before (v4.x)
  import javax.annotation.Resource;
  import javax.annotation.PostConstruct;
  import javax.validation.Valid;
  import javax.servlet.http.HttpServletRequest;

  // ✅ After (v5.0)
  import jakarta.annotation.Resource;
  import jakarta.annotation.PostConstruct;
  import jakarta.validation.Valid;
  import jakarta.servlet.http.HttpServletRequest;

  // ⚠️ 예외 — javax.sql.DataSource 는 Java SE 일부 (그대로 유지)
  import javax.sql.DataSource;  // ✅
  ```
- **연관**: `egovframe-conventions` Skill → Java EE → Jakarta EE 패키지 마이그레이션

---

### 1.2 `cannot find symbol: class Mapper` (또는 `EgovMapper`)

- **Symptom**
  ```
  [ERROR] SampleMapper.java:[5,40] cannot find symbol
    symbol:   class Mapper
    location: package org.egovframe.rte.psl.dataaccess.mapper
  ```
  또는 `@Mapper` 가 정상 import 되었는데 **런타임에 Bean 등록이 안 됨**.
- **Cause**: v5.0 부터 **매퍼 어노테이션 클래스명이 변경**됨.
  - v4.x: `org.egovframe.rte.psl.dataaccess.mapper.Mapper` (`@Mapper`)
  - v5.0: `org.egovframe.rte.psl.dataaccess.mapper.EgovMapper` (`@EgovMapper`)
- **Fix**:
  ```java
  // ❌ Before (v4.x)
  import org.egovframe.rte.psl.dataaccess.mapper.Mapper;
  @Mapper("sampleMapper")
  public interface SampleMapper { ... }

  // ✅ After (v5.0)
  import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
  @EgovMapper("sampleMapper")
  public interface SampleMapper { ... }
  ```
- **연관**: `egovframe-conventions` Skill → DAO / Data Access Layer

---

### 1.3 `cannot find symbol: class EgovTableIdGnrService`

- **Symptom**
  ```
  [ERROR] EgovConfigIdGeneration.java:[4,40] cannot find symbol
    symbol:   class EgovTableIdGnrService
    location: package org.egovframe.rte.fdl.idgnr.impl
  ```
- **Cause**: v5.0 에서 클래스명이 **`EgovTableIdGnrService` → `EgovTableIdGnrServiceImpl`** 로 변경 (Impl 접미사 추가).
- **Fix**:
  ```java
  // ❌ Before (v4.x)
  import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrService;
  EgovTableIdGnrService bean = new EgovTableIdGnrService();
  bean.afterPropertiesSet(); // v4.x 에서 수동 호출 필요했음

  // ✅ After (v5.0)
  import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
  EgovTableIdGnrServiceImpl bean = new EgovTableIdGnrServiceImpl();
  // afterPropertiesSet() 불필요 — Spring 라이프사이클이 처리
  ```
- **연관**: `docs/egovframe-architecture.md` → Bootstrap Beans → EgovConfigIdGeneration

---

### 1.4 `cannot find symbol: class EgovIdGnrStrategyImpl`

- **Symptom**
  ```
  [ERROR] EgovConfigIdGeneration.java:[5,40] cannot find symbol
    symbol:   class EgovIdGnrStrategyImpl
    location: package org.egovframe.rte.fdl.idgnr.impl
  ```
- **Cause**: `EgovIdGnrStrategyImpl` 은 **`.impl` 이 아닌 `.impl.strategy` 하위 패키지**에 위치 (v4.x, v5.0 동일).
- **Fix**:
  ```java
  // ❌ Before
  import org.egovframe.rte.fdl.idgnr.impl.EgovIdGnrStrategyImpl;
  // ✅ After
  import org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl;
  ```
- **연관**: `docs/egovframe-architecture.md` → Bootstrap Beans → EgovConfigIdGeneration

---

### 1.5 `package org.thymeleaf.spring5 does not exist`

- **Symptom**
  ```
  [ERROR] EgovConfigWeb.java:[18,20] package org.thymeleaf.spring5 does not exist
  ```
- **Cause**: Thymeleaf 3.1+ 는 Spring 6 와 결합되며 **`spring5` 패키지가 `spring6` 으로 이동**.
- **Fix**:
  ```java
  // ❌ v4.x
  import org.thymeleaf.spring5.SpringTemplateEngine;
  import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
  import org.thymeleaf.spring5.view.ThymeleafViewResolver;

  // ✅ v5.0
  import org.thymeleaf.spring6.SpringTemplateEngine;
  import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
  import org.thymeleaf.spring6.view.ThymeleafViewResolver;
  ```
- **연관**: `egovframe-conventions` Skill → Thymeleaf Spring Dialect 패키지

---

### 1.6 `incompatible types: Resource[] cannot be converted to Set<?>`

- **Symptom**
  ```
  [ERROR] EgovConfigProperties.java:[32,29] incompatible types:
    org.springframework.core.io.Resource[] cannot be converted to java.util.Set<?>
  ```
- **Cause**: `EgovPropertyServiceImpl#setExtFileName` 시그니처는 `Set<String>` (배열 아님).
  v5.0 공식 템플릿은 `setProperties(Map<String,String>)` 인라인 방식을 권장.
- **Fix**: Map 인라인 방식 (권장) 또는 Set<String> classpath 문자열.
  ```java
  // ✅ v5.0 권장 — Map 인라인
  Map<String, String> properties = new HashMap<>();
  properties.put("pageUnit", "10");
  properties.put("pageSize", "10");
  bean.setProperties(properties);

  // ✅ 파일 기반이 필요한 경우
  Set<String> files = new HashSet<>();
  files.add("classpath:egovframework/egovProps/globals.properties");
  bean.setExtFileName(files);
  ```
- **연관**: `docs/egovframe-architecture.md` → Bootstrap Beans → EgovConfigProperties

---

### 1.7 `cannot find symbol: class ExceptionHandlerService` (in `org.egovframe.rte.fdl.cmmn.exception.handler`)

- **Symptom**
  ```
  [ERROR] EgovConfigAspect.java:[5,52] cannot find symbol
    symbol:   class ExceptionHandlerService
    location: package org.egovframe.rte.fdl.cmmn.exception.handler
  ```
- **Cause**: `ExceptionHandlerService` 는 **`.exception.handler` 가 아니라 `.exception.manager` 하위 패키지**에 위치.
  같은 `cmmn.exception` 트리 안에 `handler/` 와 `manager/` 두 폴더가 있고 클래스가 헷갈리기 쉽다:
  - `handler/` → `ExceptionHandler` (인터페이스), `EgovBoardExcepHndlr` 등 핸들러 구현
  - `manager/` → `ExceptionHandlerService`, `DefaultExceptionHandleManager`, `AbstractExceptionHandleManager`
- **Fix**:
  ```java
  // ❌ Before — 잘못된 패키지
  import org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandlerService;

  // ✅ After — manager 하위
  import org.egovframe.rte.fdl.cmmn.exception.manager.ExceptionHandlerService;
  ```
  같이 쓰는 import 한 세트:
  ```java
  import org.egovframe.rte.fdl.cmmn.aspect.ExceptionTransfer;
  import org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler;             // handler 하위
  import org.egovframe.rte.fdl.cmmn.exception.manager.DefaultExceptionHandleManager; // manager 하위
  import org.egovframe.rte.fdl.cmmn.exception.manager.ExceptionHandlerService;      // manager 하위 (★)
  ```
- **연관**: `docs/egovframe-architecture.md` → EgovConfigAspect

---

### 1.8 `'dependencies.dependency.version' for org.mybatis.spring.boot:mybatis-spring-boot-starter:jar is missing`

- **Symptom**
  ```
  [ERROR] 'dependencies.dependency.version' for
    org.mybatis.spring.boot:mybatis-spring-boot-starter:jar is missing.
    @ line 91, column 21
  ```
- **Cause**: eGovFrame Boot Parent 5.0.0 은 `mybatis-spring-boot-starter` 의 버전을 **dependencyManagement 에서 관리하지 않는다**.
  Spring Boot 자체 BOM 도 mybatis-spring 만 관리할 뿐 starter 버전은 관리하지 않음.
  v4.3 의 "Parent 가 관리하므로 version 명시 제거" 가이드를 v5.0 에 그대로 적용하면 빌드 실패.
- **Fix**: Spring Boot 3.x 호환 버전인 **mybatis-spring-boot-starter 3.0.x** 를 명시.
  ```xml
  <!-- ✅ v5.0 — 반드시 명시적 버전 -->
  <dependency>
      <groupId>org.mybatis.spring.boot</groupId>
      <artifactId>mybatis-spring-boot-starter</artifactId>
      <version>3.0.3</version>
  </dependency>

  <!-- ❌ v4.3 의 2.3.1 그대로 사용 — Spring Boot 2.7 용. Boot 3.x 에선 NoClassDefFoundError 발생 -->
  <version>2.3.1</version>
  ```
- **연관**: `egovframe-conventions` Skill → Library and Extension Rules

---

### 1.9 `release version 21 not supported` (또는 `--release 21`)

- **Symptom**
  ```
  [ERROR] Failed to execute goal
    org.apache.maven.plugins:maven-compiler-plugin:3.14.0:compile (default-compile)
    on project ...: Fatal error compiling: error: release version 21 not supported
  ```
  (혹은 `error: invalid target release: 21` / IDE 에서 "Java compiler: release 21 not supported")
- **Cause**: pom.xml `<java.version>21</java.version>` 또는 `<release>21</release>` 로 설정했지만,
  실행 환경의 JDK 가 Java 17 이라 컴파일 실패. **eGovFrame 5.0 의 공식 표준은 Java 17** (Boot 3.x 의 최소 요구 버전과 일치).
  Spring Boot 3.x 자체는 Java 21 도 지원하지만, 표준프레임워크 v5.0 은 Java 17 을 표준으로 삼는다.
- **Fix**: `pom.xml` 의 Java 버전을 17 로 통일.
  ```xml
  <!-- ✅ v5.0 표준 — Java 17 -->
  <properties>
      <java.version>17</java.version>
      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <!-- ❌ Java 21 — 환경 따라 빌드 실패. v5.0 표준 아님 -->
  <properties>
      <java.version>21</java.version>
  </properties>
  ```
  `JAVA_HOME` 도 Java 17 로 맞추는 것이 안전:
  ```bash
  # macOS 예
  export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home
  export PATH=$JAVA_HOME/bin:$PATH
  java -version  # 17.0.x 확인
  ```
- **연관**: `egovframe-conventions` Skill → Spring Boot 호환성 매트릭스

---

## 2. 기동 단계

### 2.1 `NoClassDefFoundError: javax/servlet/Filter` (또는 javax.servlet.*)

- **Symptom**
  ```
  java.lang.NoClassDefFoundError: javax/servlet/Filter
      at ...
  Caused by: java.lang.ClassNotFoundException: javax.servlet.Filter
  ```
- **Cause**: Spring Boot 3.x 는 `jakarta.servlet.*` 만 제공. 의존성 중 하나가 여전히 `javax.servlet-api` 를 끌어옴 (예: 구버전 라이브러리).
- **Fix**:
  1. `mvn dependency:tree` 로 `javax.servlet:javax.servlet-api` 출처 확인.
  2. 해당 의존성을 최신 (Jakarta 호환) 버전으로 업그레이드 또는 exclusion.
  ```xml
  <dependency>
      <groupId>com.example</groupId>
      <artifactId>legacy-lib</artifactId>
      <exclusions>
          <exclusion>
              <groupId>javax.servlet</groupId>
              <artifactId>javax.servlet-api</artifactId>
          </exclusion>
      </exclusions>
  </dependency>
  ```
- **연관**: `egovframe-conventions` Skill → Spring Boot 호환성 매트릭스

---

### 2.2 `SLF4J: Class path contains multiple SLF4J bindings`

- **Symptom**
  ```
  SLF4J: Class path contains multiple SLF4J bindings.
  SLF4J: Found binding in [jar:...logback-classic-...jar!...]
  SLF4J: Found binding in [jar:...log4j-slf4j-impl-...jar!...]
  ```
- **Cause**: Spring Boot 기본 스타터는 Logback, eGovFrame 은 Log4j2 전제 → 둘이 동시 존재.
- **Fix**: `spring-boot-starter-logging` exclusion + `spring-boot-starter-log4j2` 추가.
  ```xml
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
      <artifactId>spring-boot-starter-log4j2</artifactId>
  </dependency>
  ```
- **연관**: `egovframe-conventions` Skill → Logging 스택

---

### 2.3 `Failed to parse mapping resource: '...EgovSample_Sample_SQL.xml'`

- **Symptom**
  ```
  java.io.IOException: Failed to parse mapping resource:
    'file [.../egovframework/sqlmap/example/mappers/EgovSample_Sample_SQL.xml]'
  ```
  (매퍼 XML 에서 `resultType="egovMap"` 또는 `parameterType="searchVO"` 사용 중)
- **Cause**: typeAlias 미등록. v5.0 은 `mybatis.type-aliases-package` (properties) 가 아닌 **`sql-mapper-config.xml`** 에서 정의.
- **Fix**: `sql-mapper-config.xml` 에 별칭을 명시하고, `EgovConfigMapper` 의 `setConfigLocation()` 으로 로딩.
  ```xml
  <!-- classpath:/egovframework/sqlmap/example/sql-mapper-config.xml -->
  <configuration>
      <typeAliases>
          <typeAlias alias="egovMap" type="org.egovframe.rte.psl.dataaccess.util.EgovMap"/>
          <typeAlias alias="searchVO" type="egovframework.example.sample.service.SampleDefaultVO"/>
          <typeAlias alias="sampleVO" type="egovframework.example.sample.service.SampleVO"/>
      </typeAliases>
  </configuration>
  ```
  ```java
  // EgovConfigMapper.java
  bean.setConfigLocation(resolver.getResource(
      "classpath:/egovframework/sqlmap/example/sql-mapper-config.xml"));
  ```
- **연관**: `docs/egovframe-architecture.md` → EgovConfigMapper

---

### 2.4 `No bean named 'leaveaTrace' available`

- **Symptom**
  ```
  org.springframework.beans.factory.NoSuchBeanDefinitionException:
    No bean named 'leaveaTrace' available
  ```
- **Cause**: `EgovAbstractServiceImpl` 내부에 `@Resource(name="leaveaTrace")` 주입 필드 존재.
  Bean 미등록 시 해당 클래스를 상속한 모든 Service 가 기동 실패.
- **Fix**: `EgovConfigCommon` 에 `LeaveaTrace` + `DefaultTraceHandler` + `DefaultTraceHandleManager` 3종 세트로 Bean 등록 (v5.0 권장 — 각각 별도 Bean).
  ```java
  @Configuration
  public class EgovConfigCommon {
      @Bean public AntPathMatcher antPathMatcher() { return new AntPathMatcher(); }
      @Bean public DefaultTraceHandler defaultTraceHandler() { return new DefaultTraceHandler(); }

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
- **연관**: `docs/egovframe-architecture.md` → EgovConfigCommon

---

### 2.5 `NoSuchMessageException: idgnr.message_xxx` (또는 properties.message_xxx)

- **Symptom**
  ```
  org.springframework.context.NoSuchMessageException:
    No message found under code 'idgnr.message_table_not_found' for locale 'ko_KR'.
  ```
  (egovIdGnrService 또는 propertiesService 동작 중)
- **Cause**: v5.0 의 `EgovTableIdGnrServiceImpl` / `EgovPropertyServiceImpl` 는 RTE JAR 내부의
  `org/egovframe/rte/fdl/idgnr/messages/idgnr*.properties`,
  `org/egovframe/rte/fdl/property/messages/properties*.properties`
  를 `MessageSource` basename 으로 요구. **다중 basename 등록**이 누락되면 발생.
- **Fix**: `EgovConfigCommon.messageSource()` 의 `setBasenames()` 에 RTE 메시지 경로를 모두 포함.
  ```java
  @Bean
  public ReloadableResourceBundleMessageSource messageSource() {
      ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
      ms.setBasenames(
          "classpath:/egovframework/message/message-common",
          "classpath:/org/egovframe/rte/fdl/idgnr/messages/idgnr",
          "classpath:/org/egovframe/rte/fdl/property/messages/properties"
      );
      ms.setDefaultEncoding("UTF-8");
      ms.setCacheSeconds(60);
      return ms;
  }
  ```
- **연관**: `docs/egovframe-architecture.md` → EgovConfigCommon

---

### 2.6 `ClassCastException: ClassPathResource cannot be cast to String`

- **Symptom**
  ```
  java.lang.ClassCastException:
    class org.springframework.core.io.ClassPathResource cannot be cast to class java.lang.String
    (during setExtFileName)
  ```
- **Cause**: `setExtFileName(Set<?>)` 는 형식적으로 `Set<?>` 받지만, 내부 로직은 **`Set<String>`** 전제.
  원소로 `Resource` 를 담으면 내부에서 `(String)` 캐스팅 중 실패.
- **Fix**: classpath 접두 문자열로 전달하거나, 더 단순한 `setProperties(Map<String,String>)` 방식 사용.
  ```java
  // ✅ Set<String>
  Set<String> files = new HashSet<>();
  files.add("classpath:egovframework/egovProps/globals.properties");
  bean.setExtFileName(files);

  // ✅ v5.0 공식 템플릿 권장 — Map 인라인
  Map<String, String> properties = new HashMap<>();
  properties.put("pageUnit", "10");
  bean.setProperties(properties);
  ```
- **연관**: `docs/egovframe-architecture.md` → EgovConfigProperties

---

### 2.7 `BeanCreationException: ... egovframe-rte-* 의존성 미해결`

- **Symptom**
  ```
  Could not resolve dependencies for project ...:
    Could not find artifact org.egovframe.rte:org.egovframe.rte.ptl.mvc:jar:5.0.0
  ```
- **Cause**: v4.x 의 artifactId (`org.egovframe.rte.ptl.mvc` 등) 를 v5.0 에서 그대로 사용.
  v5.0 부터 artifactId 가 **점 → 하이픈** 형식으로 변경 (`egovframe-rte-ptl-mvc`).
- **Fix**: `pom.xml` 의 모든 RTE artifactId 를 v5.0 형식으로 일괄 교체.
  ```xml
  <!-- ❌ v4.x -->
  <artifactId>org.egovframe.rte.ptl.mvc</artifactId>
  <artifactId>org.egovframe.rte.psl.dataaccess</artifactId>
  <artifactId>org.egovframe.rte.fdl.idgnr</artifactId>
  <artifactId>org.egovframe.rte.fdl.property</artifactId>

  <!-- ✅ v5.0 -->
  <artifactId>egovframe-rte-ptl-mvc</artifactId>
  <artifactId>egovframe-rte-psl-dataaccess</artifactId>
  <artifactId>egovframe-rte-fdl-idgnr</artifactId>
  <artifactId>egovframe-rte-fdl-property</artifactId>
  <artifactId>egovframe-rte-ptl-reactive</artifactId>  <!-- 신규: @EgovNullCheck 필요 시 -->
  ```
  Parent 도 함께 교체:
  ```xml
  <parent>
      <groupId>org.egovframe.boot</groupId>
      <artifactId>egovframe-boot-starter-parent</artifactId>  <!-- v4.3: org.egovframe.boot.starter.parent -->
      <version>5.0.0</version>
  </parent>
  ```
- **연관**: `egovframe-conventions` Skill → Library and Extension Rules

---

### 2.8 `#egovPaginationFormat 표현식 인식 불가` (Thymeleaf)

- **Symptom**
  ```
  org.thymeleaf.exceptions.TemplateProcessingException:
    Cannot find expression object 'egovPaginationFormat'
  ```
- **Cause**: v5.0 에서 페이징 표현식 객체명이 **`egovPaginationFormat` → `egovKrdsPaginationRenderer`** 로 변경.
  메서드도 `paginationFormat()` → `renderPagination()`.
- **Fix**:
  ```html
  <!-- ❌ v4.x -->
  <span th:utext="${#egovPaginationFormat.paginationFormat(paginationInfo, 'samplePagination')}"></span>

  <!-- ✅ v5.0 -->
  <span th:utext="${#egovKrdsPaginationRenderer.renderPagination(paginationInfo, 'samplePagination')}"></span>
  ```
  Dialect 도 새 표현식 객체명을 노출하도록 등록:
  ```java
  return Collections.singleton("egovKrdsPaginationRenderer");  // NOT "egovPaginationFormat"
  ```
- **연관**: `docs/egovframe-architecture.md` → KRDS Pagination Renderer

---

### 2.9 `BeanInstantiationException: EgovKrdsPaginationRenderer ... messageSource null`

- **Symptom**
  ```
  org.springframework.beans.BeanInstantiationException:
    Failed to instantiate [...EgovKrdsPaginationRenderer]:
    Constructor threw exception; nested exception is java.lang.NullPointerException
  ```
- **Cause**: 생성자에서 `initVariables()` 를 호출하면 `messageSource` 가 아직 주입 전이라 NPE.
- **Fix**: 생성자에서 라벨 초기화 호출 금지. `@PostConstruct` 메서드에서 처리.
  ```java
  @Component
  public class EgovKrdsPaginationRenderer extends AbstractKrdsPaginationRenderer {

      @Autowired
      private MessageSource messageSource;

      public EgovKrdsPaginationRenderer() {
          // ❌ 생성자에서 initVariables() 호출 금지 (messageSource 미주입)
      }

      @PostConstruct  // jakarta.annotation.PostConstruct
      public void initMessageLabels() {
          initVariables();
          // messageSource 활용 가능
      }
  }
  ```
- **연관**: `docs/egovframe-architecture.md` → KRDS Pagination Renderer

---

### 2.10 `Validator: messageSource not set` 또는 `@EgovNullCheck` 메시지 미해석

- **Symptom**: `@EgovNullCheck(message = "{confirm.required.name}")` 의 `{...}` 코드가 실제 메시지로 치환되지 않고 코드 그대로 노출.
- **Cause**: `LocalValidatorFactoryBean` 이 `messageSource` 와 결합되지 않음. 기본 ValidationMessages 만 조회.
- **Fix**: `EgovConfigValidation` 에서 `setValidationMessageSource(messageSource)` 명시.
  ```java
  @Configuration
  public class EgovConfigValidation {
      @Bean
      public Validator getValidator(@Qualifier("messageSource") MessageSource messageSource) {
          LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
          bean.setValidationMessageSource(messageSource);  // ★ 핵심
          return bean;
      }
  }
  ```
  추가로:
  - `pom.xml` 에 `spring-boot-starter-validation` 포함되어 있는지 확인.
  - `@EgovNullCheck` 사용 시 `egovframe-rte-ptl-reactive` 의존성 포함 확인.
- **연관**: `docs/egovframe-architecture.md` → EgovConfigValidation

---

### 2.11 `BeanDefinitionOverrideException: Invalid bean definition with name 'dataSource'`

- **Symptom**
  ```
  BeanDefinitionOverrideException: Invalid bean definition with name 'dataSource':
    Cannot register bean definition [...EmbeddedDatabaseFactoryBean...]
    for bean 'dataSource': There is already [...HikariDataSource...] bound.
  ```
- **Cause**: Spring Boot 자동 설정이 등록한 Bean 과 우리가 `@Bean` 으로 등록한 Bean 이 같은 이름으로 충돌.
- **Fix**: `application.properties` 에 오버라이딩 허용 명시.
  ```properties
  spring.main.allow-bean-definition-overriding=true
  ```
  또는 자동 설정을 제외:
  ```java
  @SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
  ```
- **연관**: `docs/egovframe-architecture.md` → application.properties

---

### 2.12 `HV000030: No validator could be found for constraint 'EgovNullCheck' validating type 'java.sql.Timestamp'`

- **Symptom**
  ```
  org.springframework.beans.factory.BeanCreationException:
    Error creating bean with name 'sampleController': ...
  Caused by: jakarta.validation.UnexpectedTypeException:
    HV000030: No validator could be found for constraint
    'org.egovframe.rte.ptl.reactive.validation.EgovNullCheck'
    validating type 'java.sql.Timestamp'. Check configuration for 'createdAt'
  ```
  (`Timestamp` 자리에 `java.math.BigInteger`, `java.math.BigDecimal`, `java.lang.Long`,
  `java.lang.Boolean`, enum, `java.util.Date`, `java.time.LocalDateTime` 등 어떤 비-String
  타입이든 동일 증상으로 표시된다. 컨텍스트가 기동조차 못 한다.)

- **Cause**: `@EgovNullCheck` 의 ConstraintValidator 는 **`String` 타입만 처리하도록 구현**되어 있다.
  AI 에이전트가 자동 생성된 VO 를 사후에 편집하며 `Timestamp createdAt`, `BigInteger id`,
  `Boolean useYn` 같은 비-String 필드에도 `@EgovNullCheck` 를 일괄 추가했을 때 흔히 발생한다.
  `egov_scaffold_project` / `egov_add_entity` 가 만든 원본 VO 는 String 필드에만 붙도록 분기
  되어 있어 정상이므로, **사후 편집으로 추가된 비-String 필드의 어노테이션** 만 의심하면 된다.

- **Fix**: 예외 메시지의 `Check configuration for '<fieldName>'` 가 가리키는 필드를 VO 에서 찾아
  `@EgovNullCheck` 를 `@NotNull` (jakarta) 로 교체한다. 숫자/날짜 도메인 제약이 필요하면
  `@Min` / `@Max` / `@PastOrPresent` 등 jakarta 표준과 조합.

  ```java
  // ❌ Before — HV000030 으로 기동 실패
  @EgovNullCheck(message = "{confirm.required.createdAt}")
  private Timestamp createdAt;

  @EgovNullCheck(message = "{confirm.required.deptId}")
  private BigInteger deptId;

  @EgovNullCheck(message = "{confirm.required.useYn}")
  private Boolean useYn;

  // ✅ After — 타입에 맞는 jakarta 어노테이션
  import jakarta.validation.constraints.NotNull;
  import jakarta.validation.constraints.Min;

  @NotNull(message = "{confirm.required.createdAt}")
  private Timestamp createdAt;

  @NotNull(message = "{confirm.required.deptId}")
  @Min(value = 0, message = "{validation.deptId.min}")
  private BigInteger deptId;

  @NotNull(message = "{confirm.required.useYn}")
  private Boolean useYn;
  ```

  String 필드는 그대로 `@EgovNullCheck` 를 유지한다 (정상 동작).

- **예방 (재발 방지)**: VO 를 편집할 때 "필수 입력 어노테이션을 일괄 추가" 가 아니라
  **필드 타입을 보고 분기**한다 — `String` → `@EgovNullCheck`, **그 외 모든 타입** → `@NotNull`.
  자세한 규칙은 `egovframe-conventions` Skill 의 **Validation Layer (VO Annotations)** 섹션 참고.

- **연관**: `egovframe-conventions` Skill → Validation Layer / `docs/egovframe-architecture.md` → EgovConfigValidation

### 2.13 POST 요청만 403 / `egovCSRFAccessDenied` 화면 표시

- **Symptom**: 목록 조회(GET)는 정상인데 등록/수정/삭제 등 POST 요청만 403 이 되거나
  "잘못된 접근입니다. (CSRF 토큰이 유효하지 않습니다)" 화면(`/egovCSRFAccessDenied.do`)으로
  넘어간다.

- **Cause**: 5.0 Boot 산출물은 CSRF 보호가 **기본 활성**(`egov.security.csrf=true`)이다.
  생성 화면은 `th:action` 없이 스크립트로 action 을 지정해 submit 하므로 Thymeleaf 의
  자동 hidden 필드 주입이 동작하지 않는다 — 생성된 화면에는 명시적 `_csrf` hidden
  필드와 meta 태그가 이미 들어 있지만, **사후에 추가한 커스텀 POST 폼/fetch 에 토큰을
  빠뜨리면** CsrfFilter 가 요청을 거부한다. 첨부파일 업로드에서 파일이
  `spring.servlet.multipart.max-file-size` 를 초과한 경우에도 multipart 파싱 실패로
  토큰을 읽지 못해 같은 화면이 나타날 수 있다 (용량 초과 안내 대신 CSRF 화면).

- **Fix**: 생성 화면의 패턴을 복제한다 —
  폼에는 `<input type="hidden" id="csrfField" th:if="${_csrf != null}"
  th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />`,
  AJAX/fetch 는 `<meta name="_csrf">` / `<meta name="_csrf_header">` 를 읽어 요청
  헤더로 전송한다. GET 으로 submit 하는 폼은 hidden 필드를 `disabled` 처리해 토큰의
  URL 유출을 막는다. 문제 격리가 필요하면 `egov.security.csrf=false` 로 일시 비활성.

- **연관**: `docs/egovframe-architecture.md` → EgovConfigWebSecurity / 생성된
  `config/EgovConfigWebSecurity.java`, `cmm/web/EgovCsrfControllerAdvice.java`

---

## 3. 체크리스트 (생성 직후 1회 기동용)

새 프로젝트를 만든 뒤 **첫 `mvn spring-boot:run` 직전**에 다음을 순차 확인:

### pom.xml
- [ ] `<parent>` = `egovframe-boot-starter-parent` **5.0.0** (NOT `org.egovframe.boot.starter.parent`)
- [ ] `<java.version>` = **17** (v5.0 표준프레임워크 기준 — 21 아님)
- [ ] `maven-compiler-plugin` `release` = `17` (또는 properties 의 `java.version` 으로만 제어)
- [ ] RTE 의존성 artifactId 가 **`egovframe-rte-*` 형식** (NOT `org.egovframe.rte.*`)
- [ ] RTE 의존성에 `<version>` 미명시 (Parent 가 관리)
- [ ] `<commons-lang3.version>` 오버라이드 **제거** (v4.3 잔재)
- [ ] `mybatis-spring-boot-starter` `<version>3.0.3</version>` **명시** (Parent 가 관리하지 않음 — v4.3 의 2.3.1 은 Boot 3.x 와 호환 안 되므로 3.0.x 사용)
- [ ] `spring-boot-starter-web` / `-thymeleaf` 에서 `starter-logging` exclusion
- [ ] `spring-boot-starter-log4j2` 추가
- [ ] `spring-boot-starter-validation` 추가
- [ ] `egovframe-rte-ptl-reactive` 추가 (`@EgovNullCheck` 사용 시)
- [ ] `egovframe` 저장소 등록 (`https://maven.egovframe.go.kr/maven/`)

### Java 소스 — Jakarta 마이그레이션
- [ ] `javax.annotation.*` → `jakarta.annotation.*` 일괄 교체
- [ ] `javax.validation.*` → `jakarta.validation.*` 일괄 교체
- [ ] `javax.servlet.*` → `jakarta.servlet.*` 일괄 교체
- [ ] `javax.persistence.*` → `jakarta.persistence.*` 일괄 교체 (JPA 사용 시)
- [ ] `org.thymeleaf.spring5.*` → `org.thymeleaf.spring6.*` 일괄 교체
- [ ] `javax.sql.DataSource` 는 그대로 유지 (Java SE)

### Java 소스 — eGovFrame 5.0 클래스/어노테이션 변경
- [ ] `@Mapper` → `@EgovMapper` (`org.egovframe.rte.psl.dataaccess.mapper.EgovMapper`)
- [ ] `EgovTableIdGnrService` → `EgovTableIdGnrServiceImpl`
- [ ] `afterPropertiesSet()` 수동 호출 제거
- [ ] `EgovIdGnrStrategyImpl` import 경로 = `.impl.strategy` 하위
- [ ] Thymeleaf 표현식: `#egovPaginationFormat.paginationFormat` → `#egovKrdsPaginationRenderer.renderPagination`

### @Configuration 클래스 (9분할 권장)
- [ ] `EgovConfigCommon` — `leaveaTrace`, `messageSource` (다중 basename), `messageSourceAccessor`
- [ ] `EgovConfigProperties` — `propertiesService` (Map 또는 Set<String>)
- [ ] `EgovConfigDatasource` — `dataSource` Bean
- [ ] `EgovConfigMapper` — `SqlSessionFactoryBean` (`name="sqlSession"`) + `@MapperScan` + `sql-mapper-config.xml`
- [ ] `EgovConfigIdGeneration` — `EgovTableIdGnrServiceImpl` + `EgovIdGnrStrategyImpl`
- [ ] `EgovConfigTransaction` — `txManager` + `txAdvice` + `txAdvisor` (Pointcut: `**impl.*Impl.*(..)`)
- [ ] `EgovConfigAspect` — `ExceptionTransfer` + `EgovAopExceptionTransfer` + 핸들러
- [ ] `EgovConfigValidation` — `LocalValidatorFactoryBean` + `setValidationMessageSource`
- [ ] `EgovConfigWebSecurity` — `SecurityFilterChain` (보안 헤더·CSRF·StrictHttpFirewall·CORS, `egov.security.*` 프로퍼티 구동)
- [ ] `EgovConfigWeb` — `@Import` 로 위 9개 결합 + Thymeleaf engine + `EgovPaginationDialect`

### application.properties
- [ ] `spring.main.allow-bean-definition-overriding=true`
- [ ] `mybatis.*` 설정 **없음** (Java Config 가 처리)
- [ ] DataSource / `db/sampledb.sql` 구성 (IDS 테이블 포함)
- [ ] `egov.security.*` 블록 — headers(sniff/xframe-options/xss-protection/cache-control), csrf(+csrf-access-denied-url), cors(기본 enabled=false)
- [ ] `spring.autoconfigure.exclude=...security.servlet.UserDetailsServiceAutoConfiguration` (인증 미도입 상태의 generated password 차단)

### 리소스 파일
- [ ] `classpath:/egovframework/sqlmap/example/sql-mapper-config.xml` — typeAliases 정의
- [ ] `classpath:/egovframework/sqlmap/example/mappers/*.xml` — 매퍼 SQL
- [ ] `classpath:/egovframework/message/message-common*.properties` — 앱 메시지
- [ ] `classpath:/db/sampledb.sql` — HSQL 스키마 + 시드 데이터 (IDS 포함)
- [ ] `classpath:/log4j2.xml` — Log4j2 설정

---
