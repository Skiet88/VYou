package za.ac.skiet.vyou2.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailer.class);

    private final JavaMailSender sender;
    private final String fromAddress;

    /**
     * {@code fromAddress} is spring.mail.username: most SMTP providers (Gmail included) require
     * the From address to be the authenticated account, so there is nothing further to configure.
     * Blank means mail isn't set up yet; every send is then a no-op (logged, not thrown).
     */
    public SmtpMailer(JavaMailSender sender, @Value("${spring.mail.username:}") String fromAddress) {
        this.sender = sender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("Mail is not configured (spring.mail.username is blank); skipping an email send");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (MailException ex) {
            // Never log the recipient address or body.
            log.warn("Failed to send an email: {}", ex.getMessage());
        }
    }
}
