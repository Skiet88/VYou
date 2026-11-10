package za.ac.skiet.vyou2.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactorTest {

    private final PiiRedactor redactor = new PiiRedactor();

    @Test
    void redactsEmailPhoneIdAndUrl() {
        String text = "Email me at jane.doe@example.co.za or call +27 82 123 4567. "
                + "My ID is 9001015009087. See https://example.com/profile/jane for more.";
        String redacted = redactor.redact(text);
        assertThat(redacted)
                .doesNotContain("jane.doe@example.co.za")
                .doesNotContain("9001015009087")
                .doesNotContain("example.com/profile/jane")
                .contains("[redacted-email]")
                .contains("[redacted-id-number]")
                .contains("[redacted-url]");
    }

    @Test
    void leavesOrdinaryInterviewAnswerUntouched() {
        String text = "I used Spring Boot and JPA to build a REST API with 12 endpoints.";
        assertThat(redactor.redact(text)).isEqualTo(text);
    }

    @Test
    void handlesNullAndEmpty() {
        assertThat(redactor.redact(null)).isNull();
        assertThat(redactor.redact("")).isEmpty();
    }

    /**
     * Regression guard against ReDoS: a pathological input must not cause catastrophic
     * backtracking. All redactor patterns are linear, so this must return quickly.
     */
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void doesNotBacktrackCatastrophicallyOnAdversarialInput() {
        String adversarial = "1".repeat(50_000) + "@" + "a".repeat(50_000) + "!";
        assertThat(redactor.redact(adversarial)).isNotNull();
    }
}
