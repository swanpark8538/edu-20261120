package egovframework.example.cmmn.config;

import java.util.Map;

import org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace;
import org.egovframe.rte.fdl.cmmn.trace.handler.DefaultTraceHandler;
import org.egovframe.rte.fdl.cmmn.trace.handler.TraceHandler;
import org.egovframe.rte.fdl.cmmn.trace.manager.DefaultTraceHandleManager;
import org.egovframe.rte.fdl.cmmn.trace.manager.TraceHandlerService;
import org.egovframe.rte.fdl.property.impl.EgovPropertyServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

/** 실행환경 공통 빈: 프로퍼티 서비스와 EgovAbstractServiceImpl 이 요구하는 leaveaTrace. */
@Configuration
public class EgovConfigCommon {

    @Bean(destroyMethod = "destroy")
    public EgovPropertyServiceImpl propertiesService() {
        EgovPropertyServiceImpl service = new EgovPropertyServiceImpl();
        service.setProperties(Map.of("pageUnit", "10", "pageSize", "10"));
        return service;
    }

    @Bean
    public LeaveaTrace leaveaTrace() {
        DefaultTraceHandleManager manager = new DefaultTraceHandleManager();
        manager.setReqExpMatcher(new AntPathMatcher());
        manager.setPatterns(new String[] {"*"});
        manager.setHandlers(new TraceHandler[] {new DefaultTraceHandler()});

        LeaveaTrace trace = new LeaveaTrace();
        trace.setTraceHandlerServices(new TraceHandlerService[] {manager});
        return trace;
    }
}
