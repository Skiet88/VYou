package za.ac.skiet.vyou2.security;

import za.ac.skiet.vyou2.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-key rate limiting (Bucket4j). This limits a single JVM: a deployment running
 * several instances behind a load balancer needs a shared store (e.g. Bucket4j's Redis backend)
 * for the limit to hold across instances. Fine for a single-instance deployment.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Limit limit = limitFor(request);
        if (limit != null) {
            Bucket bucket = buckets.computeIfAbsent(limit.key(), k -> newBucket(limit.config()));
            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/problem+json");
                response.getWriter().write(
                        "{\"title\":\"Too many requests\",\"status\":429,"
                        + "\"detail\":\"Too many requests. Please wait and try again.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Limit limitFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        // Also covers /api/account/delete: it re-checks the caller's password, so it needs the
        // same brute-force protection as login.
        if ("POST".equals(method) && (path.equals("/api/auth/login") || path.equals("/api/auth/signup")
                || path.equals("/api/account/delete") || path.equals("/api/email-verification/resend")
                || path.equals("/api/email-verification/verify"))) {
            return new Limit("auth:" + clientIp(request), properties.auth());
        }
        boolean aiEndpoint = "POST".equals(method) && path.startsWith("/api/interviews")
                && (path.equals("/api/interviews") || path.endsWith("/answers") || path.endsWith("/complete")
                    || path.endsWith("/summary"));
        if (aiEndpoint) {
            String principalKey = currentUserIdOrNull();
            // Not authenticated yet (shouldn't normally reach here once Security denies it, but
            // fail safe by IP rather than skipping the limit entirely).
            String key = principalKey != null ? principalKey : clientIp(request);
            return new Limit("ai:" + key, properties.ai());
        }
        return null;
    }

    private static String currentUserIdOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails details) {
            return details.getUser().getId().toString();
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }

    private static Bucket newBucket(RateLimitProperties.Bucket config) {
        Duration period = config.refillPeriod();
        Bandwidth limit = Bandwidth.builder()
                .capacity(config.capacity())
                .refillGreedy(config.capacity(), period)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private record Limit(String key, RateLimitProperties.Bucket config) {
    }
}
