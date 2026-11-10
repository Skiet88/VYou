package za.ac.skiet.vyou2.factory;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewStatus;
import za.ac.skiet.vyou2.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewSessionFactoryTest {

    private static final UUID USER = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void startTrimsRoleAndSetsInProgress() {
        InterviewSession s = InterviewSessionFactory.start(USER, "  Java Developer ", ExperienceLevel.JUNIOR,
                InterviewMode.TEXT, "Tell me about yourself", NOW);
        assertThat(s.getJobRole()).isEqualTo("Java Developer");
        assertThat(s.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(s.getPendingQuestion()).contains("Tell me about yourself");
    }

    @Test
    void rejectsBlankRole() {
        assertThatThrownBy(() -> InterviewSessionFactory.start(USER, " ", ExperienceLevel.JUNIOR,
                InterviewMode.TEXT, "Q", NOW)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void transitionsReturnNewInstancesWithSameId() {
        InterviewSession s = InterviewSessionFactory.start(USER, "Dev", ExperienceLevel.MID, InterviewMode.VOICE, "Q1", NOW);
        InterviewSession next = InterviewSessionFactory.withNextQuestion(s, "Q2");
        InterviewSession done = InterviewSessionFactory.complete(next, NOW.plusSeconds(60));

        assertThat(next).isNotSameAs(s);
        assertThat(next.getId()).isEqualTo(s.getId());
        assertThat(s.getPendingQuestion()).contains("Q1");
        assertThat(done.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(done.getPendingQuestion()).isEmpty();
        assertThatThrownBy(() -> InterviewSessionFactory.complete(done, NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void startRequiresUser() {
        assertThatThrownBy(() -> InterviewSessionFactory.start(null, "Dev", ExperienceLevel.JUNIOR,
                InterviewMode.TEXT, "Q", NOW)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void userEmailIsNormalisedAndValidated() {
        assertThat(UserFactory.create(" Thabo@Example.COM ", "Thabo", NOW).getEmail()).isEqualTo("thabo@example.com");
        assertThatThrownBy(() -> UserFactory.create("not-an-email", "Thabo", NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void summaryAveragesScoresAndRequiresTurns() {
        UUID sessionId = UUID.randomUUID();
        var turns = List.of(
                InterviewTurnFactory.create(sessionId, 1, "Q1", "A1", EvaluationFactory.create(6, 7, 8, 7, List.of(), List.of()), NOW),
                InterviewTurnFactory.create(sessionId, 2, "Q2", "A2", EvaluationFactory.create(7, 4, 6, 4, List.of(), List.of()), NOW));
        var summary = InterviewSummaryFactory.create(sessionId, turns, "Good effort.", List.of("Clear"), List.of("Depth"), NOW);
        assertThat(summary.getAverageClarity()).isEqualTo(6.5);
        assertThat(summary.getAverageOverall()).isEqualTo(5.5);
        assertThat(summary.getQuestionsAnswered()).isEqualTo(2);
        assertThatThrownBy(() -> InterviewSummaryFactory.create(sessionId, List.of(), "x", List.of(), List.of(), NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void evaluationScoresMustBeInRange() {
        assertThatThrownBy(() -> EvaluationFactory.create(11, 5, 5, 5, List.of(), List.of()))
                .isInstanceOf(DomainValidationException.class);
    }
}
