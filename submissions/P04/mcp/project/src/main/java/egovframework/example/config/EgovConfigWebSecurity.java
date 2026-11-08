package egovframework.example.config;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security 기반 웹 보안 설정.
 * <p>
 * 전자정부 표준프레임워크 공통컴포넌트(security 방식)의
 * {@code egov-security-config.properties} + {@code EgovSecurityConfiguration}
 * (egovframe-rte-fdl-security) 구조를 참조해, 동일한 프로퍼티 의미로
 * 보안 응답 헤더(sniff / xframeOptions / xssProtection / cacheControl)와
 * CSRF 보호(csrf / csrfAccessDeniedUrl), CORS 정책을 적용한다. 값은
 * {@code application.properties} 의 {@code egov.security.*} 에서 주입된다.
 * 아울러 FilterChainProxy 가 암묵적으로 쓰는 요청 방화벽(StrictHttpFirewall)과
 * 거부 핸들러를 명시적 빈으로 드러낸다.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class EgovConfigWebSecurity {

	@Value("${egov.security.headers.sniff:true}")
	private boolean sniff;

	@Value("${egov.security.headers.xframe-options:SAMEORIGIN}")
	private String xframeOptions;

	@Value("${egov.security.headers.xss-protection:true}")
	private boolean xssProtection;

	@Value("${egov.security.headers.cache-control:true}")
	private boolean cacheControl;

	@Value("${egov.security.csrf:true}")
	private boolean csrf;

	@Value("${egov.security.csrf-access-denied-url:/egovCSRFAccessDenied.do}")
	private String csrfAccessDeniedUrl;

	@Value("${egov.security.cors.enabled:false}")
	private boolean corsEnabled;

	@Value("${egov.security.cors.allowed-origins:}")
	private List<String> corsAllowedOrigins;

	@Value("${egov.security.cors.allowed-methods:GET,POST}")
	private List<String> corsAllowedMethods;

	@Value("${egov.security.cors.allowed-headers:*}")
	private List<String> corsAllowedHeaders;

	@Value("${egov.security.cors.allow-credentials:false}")
	private boolean corsAllowCredentials;

	@Value("${egov.security.cors.max-age:1800}")
	private long corsMaxAge;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// 스캐폴드 기본 상태는 인증 미도입 — 공통컴포넌트의 permitAllList 전체 허용에 해당.
		// 인증 도입 시 requestMatchers(...) 로 보호 대상 URL 을 좁혀 나간다.
		http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());

		configureCors(http);
		configureCsrf(http);
		configureSecurityHeaders(http);

		return http.build();
	}

	/**
	 * CORS (교차 출처 자원 공유).
	 * <p>
	 * 공통컴포넌트(security 방식)는 CORS 설정이 전혀 없어 교차 출처 요청이
	 * 암묵적으로 차단된다 — 기본값(enabled=false)은 같은 자세를 유지한다.
	 * 다른 출처의 프런트엔드/API 소비자가 필요할 때만 egov.security.cors.* 로
	 * 허용 출처를 화이트리스트 방식으로 연다.
	 * (allow-credentials=true 와 allowed-origins=* 의 조합은 Spring 이 거부한다)
	 * </p>
	 */
	private void configureCors(HttpSecurity http) throws Exception {
		if (corsEnabled) {
			http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
		} else {
			http.cors(AbstractHttpConfigurer::disable);
		}
	}

	private CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsAllowedOrigins);
		configuration.setAllowedMethods(corsAllowedMethods);
		configuration.setAllowedHeaders(corsAllowedHeaders);
		configuration.setAllowCredentials(corsAllowCredentials);
		configuration.setMaxAge(corsMaxAge);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	/**
	 * 요청 URL 방화벽 (StrictHttpFirewall).
	 * <p>
	 * 공통컴포넌트(security 방식)는 FilterChainProxy 가 내부적으로 생성하는
	 * StrictHttpFirewall 기본값에 암묵적으로 의존한다. 생성 코드는 같은 정책을
	 * 명시적 빈으로 드러낸다 — 아래 setter 들은 모두 기본값과 동일하며, 정책을
	 * 완화할 때 어떤 차단을 푸는지 코드에 남기기 위한 것이다.
	 * (세션 트래킹이 cookie 전용이므로 세미콜론(;jsessionid) 차단도 안전하다)
	 * </p>
	 */
	@Bean
	public StrictHttpFirewall httpFirewall() {
		StrictHttpFirewall firewall = new StrictHttpFirewall();
		firewall.setAllowUrlEncodedSlash(false);
		firewall.setAllowBackSlash(false);
		firewall.setAllowUrlEncodedPercent(false);
		firewall.setAllowSemicolon(false);
		firewall.setAllowUrlEncodedPeriod(false);
		firewall.setAllowUrlEncodedDoubleSlash(false);
		return firewall;
	}

	/**
	 * 방화벽 거부 요청에 대한 400 Bad Request 응답을 명시한다.
	 * FilterChainProxy 의 기본 거부 핸들러(HttpStatusRequestRejectedHandler,
	 * 기본 상태코드 400)와 동일한 동작이며, 거부 정책을 바꿀 때 손댈 지점을
	 * 코드에 드러내기 위한 빈이다.
	 */
	@Bean
	public RequestRejectedHandler requestRejectedHandler() {
		return new HttpStatusRequestRejectedHandler();
	}

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer(StrictHttpFirewall httpFirewall,
			RequestRejectedHandler requestRejectedHandler) {
		return web -> {
			web.httpFirewall(httpFirewall);
			web.requestRejectedHandler(requestRejectedHandler);
		};
	}

	/**
	 * 공통컴포넌트 egov-security-config.properties 의 csrf / csrfAccessDeniedUrl 대응.
	 * <p>
	 * csrf=true 면 Spring Security 기본 CSRF 보호(세션 저장소 + Xor 요청 핸들러)를
	 * 켜고, 토큰 검증 실패 시 안내 화면으로 포워드한다. 화면의 hidden 필드/meta
	 * 태그는 {@code EgovCsrfControllerAdvice} 가 노출하는 {@code ${_csrf}} 로 채워진다.
	 * </p>
	 */
	private void configureCsrf(HttpSecurity http) throws Exception {
		if (csrf) {
			http.csrf(Customizer.withDefaults());
			http.exceptionHandling(handling ->
					handling.accessDeniedHandler(new EgovCsrfAccessDeniedHandler(csrfAccessDeniedUrl)));
		} else {
			http.csrf(AbstractHttpConfigurer::disable);
		}
	}

	/**
	 * CSRF 토큰 누락/불일치 시 csrfAccessDeniedUrl 로 포워드하는 접근 거부 핸들러.
	 * 참조 구현(EgovAccessDeniedHandler)의 무조건 포워드 후 재포워드 결함은
	 * if/else 단일 포워드로 교정했다.
	 */
	private static final class EgovCsrfAccessDeniedHandler implements AccessDeniedHandler {

		private final String csrfAccessDeniedUrl;

		private EgovCsrfAccessDeniedHandler(String csrfAccessDeniedUrl) {
			this.csrfAccessDeniedUrl = csrfAccessDeniedUrl;
		}

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response,
				AccessDeniedException accessDeniedException) throws IOException, ServletException {
			if (accessDeniedException instanceof MissingCsrfTokenException
					|| accessDeniedException instanceof InvalidCsrfTokenException) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				request.getRequestDispatcher(csrfAccessDeniedUrl).forward(request, response);
			} else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		}
	}

	/**
	 * 공통컴포넌트 egov-security-config.properties 의 sniff / xframeOptions /
	 * xssProtection / cacheControl 과 동일한 의미의 보안 응답 헤더 매핑.
	 * <p>
	 * 단 cacheControl 은 rte-fdl-security 5.0.x 의 반전된 동작(true=비활성)을
	 * 따르지 않고 프로퍼티 이름 그대로 true=활성으로 동작한다.
	 * </p>
	 */
	private void configureSecurityHeaders(HttpSecurity http) throws Exception {
		http.headers(headers -> {
			// X-Content-Type-Options: nosniff
			if (sniff) {
				headers.contentTypeOptions(Customizer.withDefaults());
			} else {
				headers.contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable);
			}

			// X-Frame-Options: SAMEORIGIN | DISABLE(헤더 제거) | 그 외 → DENY
			if (xframeOptions != null && !xframeOptions.isBlank()) {
				switch (xframeOptions.trim().toUpperCase(Locale.ROOT)) {
					case "SAMEORIGIN" -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
					case "DISABLE" -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
					default -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
				}
			}

			// X-XSS-Protection (Spring Security 6 기본값은 "0" — 구형 XSS Auditor 비활성 권고안)
			if (xssProtection) {
				headers.xssProtection(Customizer.withDefaults());
			} else {
				headers.xssProtection(HeadersConfigurer.XXssConfig::disable);
			}

			// Cache-Control: no-cache, no-store, max-age=0, must-revalidate (+ Pragma, Expires)
			if (cacheControl) {
				headers.cacheControl(Customizer.withDefaults());
			} else {
				headers.cacheControl(HeadersConfigurer.CacheControlConfig::disable);
			}
		});
	}

}
