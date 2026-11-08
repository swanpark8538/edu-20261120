package egovframework.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 게시판 애플리케이션 시작 클래스.
 * 로그인 기능이 없으므로 기본 사용자 계정 자동 생성은 제외한다.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class EgovBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(EgovBootApplication.class, args);
    }
}
