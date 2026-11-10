package za.ac.skiet.vyou2.config;

import za.ac.skiet.vyou2.security.ActiveAccountFilter;
import za.ac.skiet.vyou2.security.CsrfCookieFilter;
import za.ac.skiet.vyou2.security.RateLimitFilter;
import za.ac.skiet.vyou2.security.RequestSizeLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * These filters are Spring beans so they can be injected, which makes Spring Boot also register
 * each one as a plain servlet filter outside Spring Security. They belong only at their explicit
 * positions inside the security filter chain (SecurityConfig), so the extra registrations are
 * turned off rather than relying on OncePerRequestFilter to skip the second run.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        return disabled(filter);
    }

    @Bean
    public FilterRegistrationBean<RequestSizeLimitFilter> requestSizeLimitFilterRegistration(RequestSizeLimitFilter filter) {
        return disabled(filter);
    }

    @Bean
    public FilterRegistrationBean<CsrfCookieFilter> csrfCookieFilterRegistration(CsrfCookieFilter filter) {
        return disabled(filter);
    }

    @Bean
    public FilterRegistrationBean<ActiveAccountFilter> activeAccountFilterRegistration(ActiveAccountFilter filter) {
        return disabled(filter);
    }

    private static <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> disabled(T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
