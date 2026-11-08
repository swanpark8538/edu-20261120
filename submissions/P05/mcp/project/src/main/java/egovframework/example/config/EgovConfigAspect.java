package egovframework.example.config;

import org.egovframe.rte.fdl.cmmn.aspect.ExceptionTransfer;
import org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler;
import org.egovframe.rte.fdl.cmmn.exception.manager.DefaultExceptionHandleManager;
import org.egovframe.rte.fdl.cmmn.exception.manager.ExceptionHandlerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.util.AntPathMatcher;

import egovframework.example.exception.EgovAopExceptionTransfer;
import egovframework.example.exception.EgovBoardExcepHndlr;
import egovframework.example.exception.EgovBoardOthersExcepHndlr;

@Configuration
@EnableAspectJAutoProxy
public class EgovConfigAspect {

	@Bean
	public EgovBoardExcepHndlr egovHandler() {
		return new EgovBoardExcepHndlr();
	}

	@Bean
	public EgovBoardOthersExcepHndlr otherHandler() {
		return new EgovBoardOthersExcepHndlr();
	}

	@Bean
	public DefaultExceptionHandleManager defaultExceptionHandleManager(AntPathMatcher antPathMatcher, EgovBoardExcepHndlr egovHandler) {
		DefaultExceptionHandleManager m = new DefaultExceptionHandleManager();
		m.setReqExpMatcher(antPathMatcher);
		m.setPatterns(new String[]{"**service.impl.*"});
		m.setHandlers(new ExceptionHandler[]{egovHandler});
		return m;
	}

	@Bean
	public DefaultExceptionHandleManager otherExceptionHandleManager(AntPathMatcher antPathMatcher, EgovBoardOthersExcepHndlr othersExcepHndlr) {
		DefaultExceptionHandleManager m = new DefaultExceptionHandleManager();
		m.setReqExpMatcher(antPathMatcher);
		m.setPatterns(new String[]{"**service.impl.*"});
		m.setHandlers(new ExceptionHandler[]{othersExcepHndlr});
		return m;
	}

	@Bean
	public ExceptionTransfer exceptionTransfer(
		@Qualifier("defaultExceptionHandleManager") DefaultExceptionHandleManager d,
		@Qualifier("otherExceptionHandleManager") DefaultExceptionHandleManager o) {
		ExceptionTransfer et = new ExceptionTransfer();
		et.setExceptionHandlerService(new ExceptionHandlerService[]{d, o});
		return et;
	}

	@Bean
	public EgovAopExceptionTransfer aopExceptionTransfer(ExceptionTransfer exceptionTransfer) {
		EgovAopExceptionTransfer a = new EgovAopExceptionTransfer();
		a.setExceptionTransfer(exceptionTransfer);
		return a;
	}

}
