package egovframework.board.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Locale;
import org.springframework.web.filter.OncePerRequestFilter;

/** Apply servlet multipart limits before CSRF reads a token from multipart parameters. */
public class MultipartGuardFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            try { request.getParts(); }
            catch (IllegalStateException e) { response.sendError(413); return; }
            catch (ServletException e) { response.sendError(400); return; }
        }
        chain.doFilter(request, response);
    }
}
