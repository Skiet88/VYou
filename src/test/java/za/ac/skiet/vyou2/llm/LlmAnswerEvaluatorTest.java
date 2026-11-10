package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.exception.LlmResponseException;
import za.ac.skiet.vyou2.domain.FeedbackMode;
import za.ac.skiet.vyou2.factory.InterviewSessionFactory;
import za.ac.skiet.vyou2.security.PiiRedactor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmAnswerEvaluatorTest {

    private final InterviewSession session = InterviewSessionFactory.start(UUID.randomUUID(),
            FeedbackMode.INSTANT, 5, "Java Developer", ExperienceLevel.JUNIOR, InterviewMode.TEXT, "Q", Instant.now());

    private static AnswerEvaluator evaluatorReturning(String raw) {
        return new LlmAnswerEvaluator((messages, jsonMode) -> raw, new PiiRedactor());
    }

    @Test
    void parsesFencedJsonAndClampsScores() {
        String raw = """
                ```json
                {"clarity": 7, "technical": 12, "confidence": -1, "overall": 6,
                 "strengths": ["Clear example"], "improvements": ["Mention trade-offs", "Be concise"],
                 "extra": "ignored"}
                ```""";
        Evaluation e = evaluatorReturning(raw).evaluate(session, "Q", "A");
        assertThat(e.getTechnicalScore()).isEqualTo(10);
        assertThat(e.getConfidenceScore()).isZero();
        assertThat(e.getImprovements()).isEqualTo("Mention trade-offs\nBe concise");
    }

    @Test
    void missingScoreIsAnError() {
        assertThatThrownBy(() -> evaluatorReturning("{\"clarity\": 5}").evaluate(session, "Q", "A"))
                .isInstanceOf(LlmResponseException.class);
    }

    @Test
    void nonJsonIsAnError() {
        assertThatThrownBy(() -> evaluatorReturning("Great answer!").evaluate(session, "Q", "A"))
                .isInstanceOf(LlmResponseException.class);
    }

    @Test
    void redactsPiiFromTheAnswerBeforeSendingItToTheModel() {
        java.util.concurrent.atomic.AtomicReference<String> sentContent = new java.util.concurrent.atomic.AtomicReference<>();
        LlmClient capturing = (messages, jsonMode) -> {
            sentContent.set(messages.get(1).content());
            return "{\"clarity\": 5, \"technical\": 5, \"confidence\": 5, \"overall\": 5, \"strengths\": [], \"improvements\": []}";
        };
        new LlmAnswerEvaluator(capturing, new PiiRedactor()).evaluate(session, "Q", "Reach me at a@b.com");
        assertThat(sentContent.get()).doesNotContain("a@b.com").contains("[redacted-email]");
    }
}
