package za.ac.skiet.vyou2.config;

import za.ac.skiet.vyou2.security.ActiveAccountFilter;
import za.ac.skiet.vyou2.security.CsrfCookieFilter;
import za.ac.skiet.vyou2.security.ProblemDetailAccessDeniedHandler;
import za.ac.skiet.vyou2.security.ProblemDetailAuthenticationEntryPoint;
import za.ac.skiet.vyou2.security.RateLimitFilter;
import za.ac.skiet.vyou2.security.RequestSizeLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;
import jakarta.servlet.DispatcherType;


/**
 * Deny-by-default: every request needs an authenticated session unless explicitly listed below.
 * Sessions are server-side (HttpSession), not JWTs: the session id cookie is httpOnly, so no
 * token is ever reachable from JavaScript, and logout invalidates it immediately server-side.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** Where the authenticated SecurityContext is stored between requests: the HttpSession. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource,
                                           RequestSizeLimitFilter requestSizeLimitFilter,
                                           RateLimitFilter rateLimitFilter,
                                           CsrfCookieFilter csrfCookieFilter,
                                           ActiveAccountFilter activeAccountFilter,
                                           ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
                                           ProblemDetailAccessDeniedHandler accessDeniedHandler) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // SPA pattern: the token lives in a readable (non-httpOnly) cookie so the frontend can
        // copy it into an X-XSRF-TOKEN request header; the cookie itself never authenticates
        // anything by itself, so this is not a secret.
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository).csrfTokenRequestHandler(csrfHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'")))
            .authorizeHttpRequests(authorize -> authorize
                // Spring Boot's error page handling forwards to the servlet container under
                // DispatcherType.ERROR (e.g. after CsrfFilter's own sendError(403)). Without this,
                // that internal forward is re-authorized as a fresh request and our
                // AuthenticationEntryPoint overwrites the original response status.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler))
            // Re-checks the account against the database before any authorization decision.
            .addFilterBefore(activeAccountFilter, AuthorizationFilter.class)
            .addFilterAfter(rateLimitFilter, AuthorizationFilter.class)
            .addFilterAfter(csrfCookieFilter, org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)
            .addFilterBefore(requestSizeLimitFilter, org.springframework.security.web.context.SecurityContextHolderFilter.class);

        return http.build();
    }
}
