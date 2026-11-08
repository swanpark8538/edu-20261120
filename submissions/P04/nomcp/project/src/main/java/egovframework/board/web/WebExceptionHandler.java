package egovframework.board.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class WebExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(WebExceptionHandler.class);
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView upload() { return error(413, "첨부 용량을 초과했습니다. 파일당 10MB, 요청당 55MB 이내로 첨부해 주세요."); }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ModelAndView method() { return error(405, "지원하지 않는 요청 방식입니다. 목록에서 다시 이용해 주세요."); }
    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView status(ResponseStatusException e) { return error(e.getStatusCode().value(), "요청한 게시물 또는 첨부파일을 찾을 수 없습니다."); }
    @ExceptionHandler({BindException.class, ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ModelAndView badRequest(Exception e) { return error(400, "입력한 요청 값이 올바르지 않습니다."); }
    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception e) {
        LOG.error("게시판 요청 처리 실패", e);
        return error(500, "요청을 처리하지 못했습니다. 잠시 후 다시 이용해 주세요.");
    }
    private ModelAndView error(int status, String message) {
        ModelAndView view = new ModelAndView("error");
        view.setStatus(HttpStatus.valueOf(status));
        view.addObject("status", status); view.addObject("message", message);
        return view;
    }
}
