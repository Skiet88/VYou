package za.ac.skiet.vyou2.security;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Role and status come only from the database, on every request, not from what was true at login:
 * - a suspended (or deleted) account is signed out on its very next request, even with a live
 *   session cookie, and gets 401;
 * - a role change in the database applies immediately, so authorization never uses a stale role.
 * Costs one primary-key lookup per authenticated request.
 */
@Component
public class ActiveAccountFilter extends OncePerRequestFilter {

    private final UserRepository users;
    private final SecurityContextRepository securityContextRepository;
    private final ProblemDetailAuthenticationEntryPoint entryPoint;

    public ActiveAccountFilter(UserRepository users, SecurityContextRepository securityContextRepository,
                               ProblemDetailAuthenticationEntryPoint entryPoint) {
        this.users = users;
        this.securityContextRepository = securityContextRepository;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails details) {
            Optional<UserAccount> current = users.findById(details.getUser().getId());
            if (current.isEmpty() || !current.get().isActive()) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                entryPoint.commence(request, response, new InsufficientAuthenticationException("Account is not active"));
                return;
            }
            UserAccount fresh = current.get();
            if (changed(details.getUser(), fresh)) {
                AppUserDetails refreshed = new AppUserDetails(fresh);
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(refreshed, null, refreshed.getAuthorities()));
                SecurityContextHolder.setContext(context);
                securityContextRepository.saveContext(context, request, response);
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean changed(UserAccount before, UserAccount after) {
        return before.getRole() != after.getRole()
                || before.getStatus() != after.getStatus()
                || before.isEmailVerified() != after.isEmailVerified()
                || !Objects.equals(before.getDisplayName(), after.getDisplayName())
                || !Objects.equals(before.getEmail(), after.getEmail());
    }
}
