package egovframework.example.cmmn;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.egovframe.rte.fdl.cmmn.aspect.ExceptionTransfer;

/**
 * 서비스 구현 클래스에서 발생한 예외를 실행환경 ExceptionTransfer 로 전달한다.
 */
@Aspect
public class AopExceptionTransfer {

    private final ExceptionTransfer exceptionTransfer;

    public AopExceptionTransfer(ExceptionTransfer exceptionTransfer) {
        this.exceptionTransfer = exceptionTransfer;
    }

    @Pointcut("execution(* egovframework.example..impl.*Impl.*(..))")
    private void serviceMethod() {
    }

    @AfterThrowing(pointcut = "serviceMethod()", throwing = "exception")
    public void transfer(JoinPoint joinPoint, Exception exception) throws Exception {
        exceptionTransfer.transfer(joinPoint, exception);
    }
}
