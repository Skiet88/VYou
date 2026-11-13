package za.ac.skiet.vyou2.exception;

/** The token in a password-reset link doesn't match any account, or has expired. */
public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }
}
