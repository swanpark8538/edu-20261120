package egovframework.example.cmm.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 화면(Thymeleaf)에서 {@code ${_csrf}} 표현식을 쓸 수 있도록 CSRF 토큰을 모델에
 * 노출한다. 공통컴포넌트(security 방식)의 {@code EgovCsrfControllerAdvice} 대응.
 * <p>
 * 생성 화면은 {@code th:action} 없이 스크립트로 action 을 지정해 submit 하므로
 * Thymeleaf 의 자동 hidden 필드 주입이 동작하지 않는다 — 각 폼의 명시적 hidden
 * 필드와 meta 태그가 이 모델 속성을 사용한다. csrf 비활성 시 null 을 반환하며
 * 화면의 {@code th:if="${_csrf != null}"} 가드가 조용히 생략한다.
 * </p>
 * <p>
 * 토큰은 CsrfFilter 가 요청 속성으로 노출한 것만 읽는다. 세션 저장소의 원본
 * 토큰을 직접 읽으면 Spring Security 6 기본 Xor 마스킹 핸들러와 어긋날 수 있다.
 * </p>
 */
@ControllerAdvice
public class EgovCsrfControllerAdvice {

	@Value("${egov.security.csrf:true}")
	private boolean csrf;

	@ModelAttribute("_csrf")
	public CsrfToken csrfToken(HttpServletRequest request) {
		if (!csrf) {
			return null;
		}

		Object tokenAttribute = request.getAttribute(CsrfToken.class.getName());
		if (tokenAttribute instanceof CsrfToken token) {
			return token;
		}

		Object csrfAttribute = request.getAttribute("_csrf");
		if (csrfAttribute instanceof CsrfToken token) {
			return token;
		}

		return null;
	}

}
