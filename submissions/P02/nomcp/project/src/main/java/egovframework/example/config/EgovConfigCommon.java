package egovframework.example.config;

import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler;
import org.egovframe.rte.fdl.cmmn.exception.manager.DefaultExceptionHandleManager;
import org.egovframe.rte.fdl.cmmn.exception.manager.ExceptionHandlerService;
import org.egovframe.rte.fdl.cmmn.aspect.ExceptionTransfer;
import org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace;
import org.egovframe.rte.fdl.cmmn.trace.handler.DefaultTraceHandler;
import org.egovframe.rte.fdl.cmmn.trace.handler.TraceHandler;
import org.egovframe.rte.fdl.cmmn.trace.manager.DefaultTraceHandleManager;
import org.egovframe.rte.fdl.cmmn.trace.manager.TraceHandlerService;
import org.egovframe.rte.fdl.property.impl.EgovPropertyServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.AntPathMatcher;

import egovframework.example.cmmn.AopExceptionTransfer;
import egovframework.example.cmmn.EgovExcepHndlr;

/**
 * 실행환경 공통 기반 서비스(메시지, 추적, 예외 후처리, 프로퍼티) 설정.
 */
@Configuration
public class EgovConfigCommon {

    private static final String[] ALL_PACKAGES = {"**"};

    /** EgovAbstractServiceImpl 과 ID Generation 서비스가 이 이름의 빈을 사용한다. */
    @Bean(name = "messageSource")
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames(
                "classpath:/egovframework/message/message-board",
                "classpath:/messages/message-common",
                "classpath:/org/egovframe/rte/fdl/idgnr/messages/idgnr",
                "classpath:/org/egovframe/rte/fdl/property/messages/properties");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setCacheSeconds(-1);
        return source;
    }

    @Bean
    public DefaultTraceHandleManager traceHandleManager() {
        DefaultTraceHandleManager manager = new DefaultTraceHandleManager();
        manager.setReqExpMatcher(new AntPathMatcher());
        manager.setPatterns(ALL_PACKAGES);
        manager.setHandlers(new TraceHandler[] {new DefaultTraceHandler()});
        return manager;
    }

    /** EgovAbstractServiceImpl 이 이 이름의 빈을 사용한다. */
    @Bean(name = "leaveaTrace")
    public LeaveaTrace leaveaTrace(DefaultTraceHandleManager traceHandleManager) {
        LeaveaTrace leaveaTrace = new LeaveaTrace();
        leaveaTrace.setTraceHandlerServices(new TraceHandlerService[] {traceHandleManager});
        return leaveaTrace;
    }

    @Bean
    public DefaultExceptionHandleManager exceptionHandleManager() {
        DefaultExceptionHandleManager manager = new DefaultExceptionHandleManager();
        manager.setReqExpMatcher(new AntPathMatcher());
        manager.setPatterns(ALL_PACKAGES);
        manager.setHandlers(new ExceptionHandler[] {new EgovExcepHndlr()});
        return manager;
    }

    @Bean
    public ExceptionTransfer exceptionTransfer(DefaultExceptionHandleManager exceptionHandleManager) {
        ExceptionTransfer transfer = new ExceptionTransfer();
        transfer.setExceptionHandlerService(new ExceptionHandlerService[] {exceptionHandleManager});
        return transfer;
    }

    @Bean
    public AopExceptionTransfer aopExceptionTransfer(ExceptionTransfer exceptionTransfer) {
        return new AopExceptionTransfer(exceptionTransfer);
    }

    @Bean(name = "propertiesService", destroyMethod = "destroy")
    public EgovPropertyServiceImpl propertiesService(
            @Value("${Globals.pageUnit}") String pageUnit,
            @Value("${Globals.pageSize}") String pageSize) {
        Map<String, String> properties = new HashMap<>();
        properties.put("pageUnit", pageUnit);
        properties.put("pageSize", pageSize);

        EgovPropertyServiceImpl service = new EgovPropertyServiceImpl();
        service.setProperties(properties);
        return service;
    }
}
