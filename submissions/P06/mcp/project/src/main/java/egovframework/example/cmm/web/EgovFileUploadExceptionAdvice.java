package egovframework.example.cmm.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 첨부파일 업로드 관련 예외를 사용자 친화적으로 처리하는 advice.
 *
 * 처리 대상:
 * - {@link MaxUploadSizeExceededException}: spring.servlet.multipart.max-* 초과
 * - {@link SecurityException}: {@code EgovFileMngUtil} 의 실행파일 차단 / path traversal 가드
 *
 * 처리 방식 (디자인 결정):
 * - **redirect 가 아니라 inline view 로 직접 렌더링** ({@code cmm/uploadError}).
 *   redirect:referer 패턴은 referer URL 의 컨트롤러가 GET 매핑을 갖추고 있어야 동작하지만,
 *   eGovFrame 의 전통적 패턴(*View.do 가 POST-only)에서는 GET 으로 다시 진입할 수 없어
 *   {@code HttpRequestMethodNotSupportedException} (405) 이 발생한다. 도메인 컨트롤러의
 *   매핑에 의존하지 않도록 본 advice 는 자체 view 를 가진다.
 * - 폼 데이터는 보존하지 못한다 (트레이드오프). 정상 케이스가 아니라 예외 상황이므로
 *   안정적 메시지 표시 > 폼 보존을 선택.
 * - view 는 {@code cmm/uploadError.html} 이며 모델 속성 {@code errorMessage} 를 메시지 번들로
 *   해석해 표시 ({@code fail.common.fileSize.exceeded} / {@code fail.common.fileType.blocked}).
 *
 * 우선순위:
 * - {@code @ControllerAdvice} 의 {@code @ExceptionHandler} 는 {@code ExceptionHandlerExceptionResolver}
 *   에서 처리되며, {@code SimpleMappingExceptionResolver} 보다 *먼저* 동작한다.
 *   따라서 EgovConfigWeb 의 일반 에러 페이지 매핑이 있어도 본 advice 가 우선 잡는다.
 * - {@code @Order(HIGHEST_PRECEDENCE)} 로 다른 advice 보다 먼저 동작하도록 명시.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EgovFileUploadExceptionAdvice {

	private static final Logger LOGGER = LoggerFactory.getLogger(EgovFileUploadExceptionAdvice.class);

	/** view name (Thymeleaf resolver 가 templates/thymeleaf/cmm/uploadError.html 로 해석). */
	private static final String VIEW_NAME = "cmm/uploadError";

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ModelAndView handleMaxUploadSize(MaxUploadSizeExceededException ex,
			HttpServletRequest req) {
		LOGGER.warn("Upload size exceeded for {}: {}", req.getRequestURI(), ex.getMessage());
		return renderError("fail.common.fileSize.exceeded");
	}

	@ExceptionHandler(SecurityException.class)
	public ModelAndView handleSecurity(SecurityException ex, HttpServletRequest req) {
		// SecurityException 메시지에는 사용자 입력(파일명)이 들어있어 그대로 화면에 노출하지 않고
		// 메시지 키만 전달한다. 자세한 명세는 서버 로그로.
		LOGGER.warn("File upload blocked by security policy on {}: {}", req.getRequestURI(), ex.getMessage());
		return renderError("fail.common.fileType.blocked");
	}

	private static ModelAndView renderError(String messageKey) {
		ModelAndView mav = new ModelAndView(VIEW_NAME);
		mav.addObject("errorMessage", messageKey);
		return mav;
	}
}
