package egovframework.example.cmmn.web;

import org.egovframe.rte.fdl.cmmn.exception.BaseException;
import org.egovframe.rte.fdl.cmmn.exception.EgovBizException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartException;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 컨트롤러 예외를 내부 정보가 없는 공통 오류 화면으로 변환한다.
 */
@ControllerAdvice
public class EgovExceptionAdvice {

    private static final String ERROR_VIEW = "error";
    private static final String NOT_FOUND_SUFFIX = ".notFound";

    @Resource(name = "messageSource")
    private MessageSource messageSource;

    @ExceptionHandler(EgovBizException.class)
    public String handleBizException(EgovBizException exception, HttpServletResponse response, Model model) {
        String key = exception.getMessageKey();
        HttpStatus status = key != null && key.endsWith(NOT_FOUND_SUFFIX) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        response.setStatus(status.value());
        model.addAttribute("status", status.value());
        model.addAttribute("errorMessage", exception.getMessage());
        return ERROR_VIEW;
    }

    @ExceptionHandler(BaseException.class)
    public String handleBaseException(HttpServletResponse response, Model model) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ERROR_VIEW;
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String handleMultipartException(Model model) {
        model.addAttribute("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
        model.addAttribute("errorMessage",
                messageSource.getMessage("fail.file.tooLarge", null, LocaleContextHolder.getLocale()));
        return ERROR_VIEW;
    }
}
