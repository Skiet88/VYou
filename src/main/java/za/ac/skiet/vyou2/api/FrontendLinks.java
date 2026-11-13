package za.ac.skiet.vyou2.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The origin the frontend is actually reachable at, for building links inside emails. Since the
 * backend serves the built frontend from its own origin in production (see SpaConfig), this is
 * normally just derived from the incoming request. vyou.frontend-base-url overrides it for setups
 * where that would be wrong (e.g. a reverse proxy that doesn't forward the original host, or local
 * testing where the frontend runs on a different dev-server port than the backend).
 */
@Component
public class FrontendLinks {

    private final String configuredOrigin;

    public FrontendLinks(@Value("${vyou.frontend-base-url:}") String configuredOrigin) {
        this.configuredOrigin = configuredOrigin.strip().replaceAll("/+$", "");
    }

    public String origin(HttpServletRequest request) {
        if (!configuredOrigin.isBlank()) {
            return configuredOrigin;
        }
        return ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
    }
}
