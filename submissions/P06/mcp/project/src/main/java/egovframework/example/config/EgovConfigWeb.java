package egovframework.example.config;

import java.util.List;
import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import egovframework.example.pagination.EgovKrdsPaginationRenderer;
import egovframework.example.pagination.EgovPaginationDialect;

/**
 * 명시적 @Import 는 1차 스캐폴드(primary) 엔티티의 설정 클래스만 나열한다.
 * egov_add_entity 가 나중에 추가하는 EgovConfig{OtherEntity}IdGeneration 등은
 * @SpringBootApplication 의 컴포넌트 스캔이 {basePackage}.config 패키지에서
 * 자동으로 잡아주므로 이 목록에 추가하지 않아도 된다.
 */
@Configuration
@Import({
		EgovConfigAspect.class,
		EgovConfigCommon.class,
		EgovConfigDatasource.class,
		EgovConfigBoardIdGeneration.class,
		EgovConfigMapper.class,
		EgovConfigProperties.class,
		EgovConfigTransaction.class,
		EgovConfigValidation.class,
		EgovConfigWebSecurity.class
})
public class EgovConfigWeb implements WebMvcConfigurer, ApplicationContextAware {

	private ApplicationContext applicationContext;

	public void setApplicationContext(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	public SpringResourceTemplateResolver templateResolver() {
		SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
		templateResolver.setApplicationContext(this.applicationContext);
		templateResolver.setPrefix("classpath:/templates/thymeleaf/");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setCacheable(true);
		return templateResolver;
	}

	@Bean
	public SpringTemplateEngine templateEngine(EgovKrdsPaginationRenderer egovKrdsPaginationRenderer) {
		SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		templateEngine.setTemplateResolver(templateResolver());
		templateEngine.setEnableSpringELCompiler(true);
		templateEngine.addDialect(new EgovPaginationDialect(egovKrdsPaginationRenderer));
		return templateEngine;
	}

	@Bean
	public ThymeleafViewResolver thymeleafViewResolver(EgovKrdsPaginationRenderer egovKrdsPaginationRenderer) {
		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
		viewResolver.setCharacterEncoding("UTF-8");
		viewResolver.setTemplateEngine(templateEngine(egovKrdsPaginationRenderer));
		return viewResolver;
	}

	/**
	 * 정적 리소스 핸들러에는 cachePeriod 를 반드시 부여한다 — EgovConfigWebSecurity 의
	 * Cache-Control 보안 헤더(no-store 계열)는 응답에 캐시 헤더가 없을 때만 쓰이므로,
	 * 여기서 자체 Cache-Control 을 설정해야 정적 자산 캐싱이 유지된다.
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/").setCachePeriod(3600);
		registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/").setCachePeriod(3600);
		registry.addResourceHandler("/img/**").addResourceLocations("classpath:/static/img/").setCachePeriod(3600);
		registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/").setCachePeriod(3600);
		registry.addResourceHandler("/fonts/**").addResourceLocations("classpath:/static/fonts/").setCachePeriod(3600);
		registry.addResourceHandler("/favicon.ico")
				.addResourceLocations("classpath:/static/")
				.setCachePeriod(3600);
	}

	@Bean
	public SessionLocaleResolver localeResolver() {
		return new SessionLocaleResolver();
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
		interceptor.setParamName("language");
		return interceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localeChangeInterceptor());
	}

	/**
	 * ⚠️  반드시 {@code extendHandlerExceptionResolvers} 를 사용해야 한다 — {@code configureHandlerExceptionResolvers}
	 * 를 override 하면 Spring 의 기본 resolver 들({@code ExceptionHandlerExceptionResolver},
	 * {@code ResponseStatusExceptionResolver}, {@code DefaultHandlerExceptionResolver}) 이 등록되지 않아서
	 * {@code @ControllerAdvice} + {@code @ExceptionHandler} 가 모두 무력화된다 (대표적으로 첨부파일
	 * {@code EgovFileUploadExceptionAdvice} 가 작동하지 않게 된다).
	 *
	 * extend 를 쓰면 기본 resolver 들이 먼저 등록되어 {@code @ExceptionHandler} 가 정상 동작하고,
	 * 본 메서드에서 추가한 {@code SimpleMappingExceptionResolver} 는 advice 가 잡지 못한 예외
	 * (DataAccessException / EgovBizException 등) 의 fallback 으로만 동작한다.
	 */
	@Override
	public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
		Properties prop = new Properties();
		prop.setProperty("org.springframework.dao.DataAccessException", "board/egovSampleError");
		prop.setProperty("org.springframework.transaction.TransactionException", "board/egovSampleError");
		prop.setProperty("org.egovframe.rte.fdl.cmmn.exception.EgovBizException", "board/egovSampleError");
		prop.setProperty("org.springframework.security.AccessDeniedException", "board/egovSampleError");
		prop.setProperty("java.lang.Throwable", "board/egovSampleError");

		Properties statusCode = new Properties();
		statusCode.setProperty("board/egovSampleError", "400");
		statusCode.setProperty("board/egovSampleError", "500");

		SimpleMappingExceptionResolver smer = new SimpleMappingExceptionResolver();
		smer.setDefaultErrorView("board/egovSampleError");
		smer.setExceptionMappings(prop);
		smer.setStatusCodes(statusCode);
		resolvers.add(smer);
	}

}
