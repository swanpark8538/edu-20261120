package egovframework.board.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AppConfig {
    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<MultipartGuardFilter> multipartGuard() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(new MultipartGuardFilter());
        registration.setOrder(-101); // Parse bounded multipart data before Spring Security resolves the CSRF token.
        return registration;
    }
    @Bean
    public org.springframework.context.MessageSource messageSource() {
        var source = new org.springframework.context.support.ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:org/egovframe/rte/fdl/idgnr/messages/idgnr", "classpath:messages/message-common");
        source.setDefaultEncoding("UTF-8");
        return source;
    }
    @Bean
    public org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace leaveaTrace() {
        return new org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace();
    }
    @Bean(name = {"sqlSession", "sqlSessionFactory"})
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        return bean.getObject();
    }

    @Bean(destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl boardIdGnrService(DataSource dataSource) {
        EgovTableIdGnrServiceImpl service = new EgovTableIdGnrServiceImpl();
        service.setDataSource(dataSource);
        service.setTable("IDS");
        service.setTableName("BOARD");
        service.setBlockSize(1);
        return service;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data:; script-src 'none'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'"))
                .frameOptions(frame -> frame.deny()));
        // CSRF protection stays enabled, including multipart writes.
        return http.build();
    }
}
