package egovframework.example.config;

import org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace;
import org.egovframe.rte.fdl.cmmn.trace.handler.DefaultTraceHandler;
import org.egovframe.rte.fdl.cmmn.trace.handler.TraceHandler;
import org.egovframe.rte.fdl.cmmn.trace.manager.DefaultTraceHandleManager;
import org.egovframe.rte.fdl.cmmn.trace.manager.TraceHandlerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.AntPathMatcher;

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
		ReloadableResourceBundleMessageSource m = new ReloadableResourceBundleMessageSource();
		m.setBasenames(
				"classpath:/egovframework/message/message-common",
				"classpath:/org/egovframe/rte/fdl/idgnr/messages/idgnr",
				"classpath:/org/egovframe/rte/fdl/property/messages/properties");
		m.setDefaultEncoding("UTF-8");
		m.setCacheSeconds(60);
		return m;
	}

	@Bean
	public MessageSourceAccessor messageSourceAccessor() {
		return new MessageSourceAccessor(this.messageSource());
	}

	@Bean
	public DefaultTraceHandleManager traceHandlerService() {
		DefaultTraceHandleManager m = new DefaultTraceHandleManager();
		m.setReqExpMatcher(antPathMatcher());
		m.setPatterns(new String[]{"*"});
		m.setHandlers(new TraceHandler[]{defaultTraceHandler()});
		return m;
	}

	@Bean
	public LeaveaTrace leaveaTrace() {
		LeaveaTrace l = new LeaveaTrace();
		l.setTraceHandlerServices(new TraceHandlerService[]{traceHandlerService()});
		return l;
	}

}
