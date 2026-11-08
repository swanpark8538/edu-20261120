package egovframework.example.cmm.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CSRF 토큰 검증 실패 시 {@code EgovConfigWebSecurity} 의 접근 거부 핸들러가
 * 포워드하는 안내 화면. 공통컴포넌트(security 방식)의
 * {@code /egovCSRFAccessDenied.do} (EgovComIndexController) 대응.
 */
@Controller
public class EgovCsrfAccessDeniedController {

	@RequestMapping("/egovCSRFAccessDenied.do")
	public String egovCSRFAccessDenied() {
		return "cmm/error/csrfAccessDenied";
	}

}
