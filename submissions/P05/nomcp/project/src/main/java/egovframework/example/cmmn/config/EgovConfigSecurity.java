package egovframework.example.cmmn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * 요구사항에 로그인이 없어 인가는 열어 두고, CSRF 토큰 검증과 보안 응답 헤더를 적용한다.
 */
@Configuration
public class EgovConfigSecurity {

    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; "
            + "img-src 'self' data:; "
            + "object-src 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'; "
            + "frame-ancestors 'none'";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 속성 이름을 null 로 두면 토큰을 요청 시작 시 읽는다.
        // 응답이 커밋된 뒤 세션을 만들다 실패하는 지연 로딩 문제를 막는다.
        XorCsrfTokenRequestAttributeHandler csrfHandler = new XorCsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(csrfHandler)
                // 데이터를 바꾸지 않는 화면 조회 요청이다. 세션 만료 뒤 목록에서 눌러도 403 이 나지 않고 새 토큰을 받는다.
                .ignoringRequestMatchers("/addBoardView.do", "/updateBoardView.do"))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN)))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
