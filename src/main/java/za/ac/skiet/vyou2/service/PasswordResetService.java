package za.ac.skiet.vyou2.service;

public interface PasswordResetService {

    /**
     * Sends a reset link if {@code email} belongs to an account; silently does nothing otherwise,
     * so this can never be used to probe which emails are registered.
     */
    void requestReset(String email, String frontendOrigin);

    /** Sets the new password and ends every other session for the account. */
    void resetPassword(String rawToken, String newRawPassword);
}
