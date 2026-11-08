package egovframework.example.cmmn.web;

import org.egovframe.rte.fdl.cmmn.exception.EgovBizException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

/** 업무 예외를 공통 오류 화면으로 보낸다. 그 밖의 예외는 Boot 오류 경로가 내부 정보 없이 처리한다. */
@ControllerAdvice
public class EgovExceptionAdvice {

    @ExceptionHandler(EgovBizException.class)
    public ModelAndView handleBizException(EgovBizException exception) {
        return errorView(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    public ModelAndView handleMultipartException() {
        return errorView(HttpStatus.PAYLOAD_TOO_LARGE, "첨부파일을 처리할 수 없습니다. 파일 크기를 확인해 주세요.");
    }

    private ModelAndView errorView(HttpStatus status, String message) {
        ModelAndView view = new ModelAndView("error");
        view.setStatus(status);
        view.addObject("status", status.value());
        view.addObject("message", message);
        return view;
    }
}
