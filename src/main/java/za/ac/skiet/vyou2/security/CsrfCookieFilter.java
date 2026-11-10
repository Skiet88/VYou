package za.ac.skiet.vyou2.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 6 resolves the CSRF token lazily; without touching it, the cookie never gets
 * written. This forces resolution on every request so the frontend always has a fresh token to
 * echo back in the X-XSRF-TOKEN header on its next state-changing request.
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        request.getAttribute(CsrfToken.class.getName());
        chain.doFilter(request, response);
    }
}
