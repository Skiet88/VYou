package za.ac.skiet.vyou2.mail;

/**
 * Sends a plain-text email. Best-effort: a failed or skipped send never throws, so a mail outage
 * or missing configuration never blocks the caller (matching how an unavailable AI provider is
 * handled elsewhere in this app).
 */
public interface Mailer {
    void send(String to, String subject, String body);
}
