package za.ac.skiet.vyou2.exception;

/** The user re-entered their password (e.g. to confirm account deletion) and it didn't match. */
public class IncorrectPasswordException extends RuntimeException {
    public IncorrectPasswordException(String message) {
        super(message);
    }
}
