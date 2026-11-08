package egovframework.board.web;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletResponse;
@ControllerAdvice
public class ErrorHandler {
 private static final Logger log=LoggerFactory.getLogger(ErrorHandler.class);
 @ExceptionHandler(ResponseStatusException.class) String known(ResponseStatusException ex,HttpServletResponse response,Model model){
  response.setStatus(ex.getStatusCode().value());model.addAttribute("errorMessage",ex.getReason());return "error";
 }
 @ExceptionHandler({MaxUploadSizeExceededException.class,org.springframework.web.multipart.MultipartException.class}) String upload(Exception ex,HttpServletResponse response,Model model){
  response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());model.addAttribute("errorMessage","파일 크기 또는 개수 제한을 초과했습니다. 개당 10MB, 요청당 45MB, 10개 이내로 첨부해 주세요.");return "error";
 }
 @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class}) String bad(Exception ex,HttpServletResponse response,Model model){
  response.setStatus(400);model.addAttribute("errorMessage","요청 값을 확인해 주세요.");return "error";
 }
 @ExceptionHandler(Exception.class) String unexpected(Exception ex,HttpServletResponse response,Model model){
  if(ex instanceof org.springframework.web.ErrorResponse error){
   response.setStatus(error.getStatusCode().value());model.addAttribute("errorMessage","요청한 주소 또는 요청 방식을 확인해 주세요.");return "error";
  }
  log.error("요청 처리 실패",ex);response.setStatus(500);model.addAttribute("errorMessage","처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");return "error";
 }
}
