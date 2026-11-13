package za.ac.skiet.vyou2.service;

import java.util.UUID;

public interface EmailVerificationService {

    /**
     * Generates a fresh token, saves it, and emails a verification link built from
     * {@code frontendOrigin} (e.g. "https://example.com", no trailing slash). A no-op if the
     * account is already verified, so both signup and a user-requested resend can call this
     * safely.
     */
    void sendVerificationEmail(UUID userId, String frontendOrigin);

    /** @throws za.ac.skiet.vyou2.exception.InvalidVerificationTokenException if the token doesn't match or has expired */
    void verify(String rawToken);
}
