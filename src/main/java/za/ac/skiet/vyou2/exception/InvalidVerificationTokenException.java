package za.ac.skiet.vyou2.exception;

/** The token in a verification link doesn't match any account, or has expired. */
public class InvalidVerificationTokenException extends RuntimeException {
    public InvalidVerificationTokenException(String message) {
        super(message);
    }
}
