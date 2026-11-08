package egovframework.example.cmmn;

import org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 서비스 예외 후처리기. 발생 위치와 예외 종류를 기록한다.
 */
public class EgovExcepHndlr implements ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EgovExcepHndlr.class);

    @Override
    public void occur(Exception exception, String packageName) {
        LOGGER.warn("service exception at {}: {}", packageName, exception.getClass().getName());
    }
}
