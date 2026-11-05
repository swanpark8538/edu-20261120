package egovframework.example.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에 현재 요청 URI 를 `egovRequestUri` 모델 속성으로 노출한다.
 *
 * 배경: Thymeleaf 3.1 부터 `#request`, `#httpServletRequest` 등 웹 표현식 객체가
 * 기본 비활성화되어, 뷰에서 직접 요청 URI 를 참조하면 SpringEL 평가 오류가 난다.
 * (참고: https://www.thymeleaf.org/doc/articles/thymeleaf31whatsnew.html)
 *
 * 이 ControllerAdvice 는 그 우회용 — `_fragments/nav.html` 의 활성 메뉴 판별에
 * 사용된다. 다른 뷰에서도 `${egovRequestUri}` 로 안전하게 현재 경로를 읽을 수 있다.
 */
@ControllerAdvice
public class EgovNavModelAdvice {

	@ModelAttribute("egovRequestUri")
	public String egovRequestUri(HttpServletRequest request) {
		return request.getRequestURI();
	}
}
