package za.ac.skiet.vyou2.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Blocks an authenticated-but-unverified account from every API endpoint except the handful it
 * needs while unverified (auth itself, deleting its own account, and verifying/resending).
 * Fail-closed by design: any future API endpoint is blocked for an unverified user unless
 * explicitly added to the exemption list below.
 *
 * Only applies to /api/**: a plain page load (e.g. refreshing /interviews) must still get the SPA
 * shell from SpaConfig, not a problem+json body, so the frontend's own route guard can read
 * GET /api/auth/me and redirect to the pending-verification page itself. Without this, every new
 * signup - unverified by default - would be unable to load any page at all.
 *
 * Runs after ActiveAccountFilter, so it always sees a freshly refreshed emailVerified flag -
 * verifying in one tab is reflected on this session's very next request, with no new login needed.
 */
@Component
public class EmailVerificationFilter extends OncePerRequestFilter {

    private static final String CODE_PROPERTY = "code";
    private static final String CODE_VALUE = "EMAIL_NOT_VERIFIED";

    private final RequestMatcher api;
    private final List<RequestMatcher> exempt;
    private final ObjectMapper mapper = new ObjectMapper();

    public EmailVerificationFilter() {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        this.api = paths.matcher("/api/**");
        this.exempt = List.of(
                paths.matcher("/api/auth/**"),
                paths.matcher("/api/account/**"),
                paths.matcher("/api/email-verification/**"),
                paths.matcher("/api/password-reset/**"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails details
                && !details.getUser().isEmailVerified() && api.matches(request)
                && exempt.stream().noneMatch(m -> m.matches(request))) {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                    "Verify your email before continuing. Check your inbox, or request a new verification email.");
            pd.setTitle("Forbidden");
            pd.setProperty(CODE_PROPERTY, CODE_VALUE);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/problem+json");
            mapper.writeValue(response.getWriter(), pd);
            return;
        }
        chain.doFilter(request, response);
    }
}
