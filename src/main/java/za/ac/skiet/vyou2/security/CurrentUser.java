package za.ac.skiet.vyou2.security;

import org.springframework.security.core.Authentication;

import java.util.UUID;

/** Reads the authenticated user's id out of the security context; never trust an id in a request body. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails details)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return details.getUser().getId();
    }
}
