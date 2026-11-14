package za.ac.skiet.vyou2.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Sends email over HTTPS via Resend's API, not SMTP: most PaaS hosts (Render included) block
 * outbound SMTP (ports 25/465/587) to curb spam, so a direct-SMTP {@code Mailer} would silently
 * time out on every send there. {@code baseUrl} is overridable so tests can point this at a local
 * stub instead of the real API.
 */
@Component
public class ResendMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(ResendMailer.class);

    private final RestClient client;
    private final String apiKey;
    private final String fromAddress;

    public ResendMailer(RestClient.Builder builder, @Value("${resend.base-url:https://api.resend.com}") String baseUrl,
                        @Value("${resend.api-key:}") String apiKey, @Value("${vyou.mail-from:}") String fromAddress) {
        this.client = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (apiKey.isBlank() || fromAddress.isBlank()) {
            log.warn("Mail is not configured (resend.api-key or vyou.mail-from is blank); skipping an email send");
            return;
        }
        try {
            client.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(Map.of("from", fromAddress, "to", to, "subject", subject, "text", body))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            // Never log the recipient address or body.
            log.warn("Failed to send an email: {}", ex.getMessage());
        }
    }
}
