package za.ac.skiet.vyou2.factory;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewStatus;
import za.ac.skiet.vyou2.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewSessionFactoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void startTrimsRoleAndSetsInProgress() {
        InterviewSession s = InterviewSessionFactory.start("  Java Developer ", ExperienceLevel.JUNIOR,
                InterviewMode.TEXT, "Tell me about yourself", NOW);
        assertThat(s.getJobRole()).isEqualTo("Java Developer");
        assertThat(s.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(s.getPendingQuestion()).contains("Tell me about yourself");
    }

    @Test
    void rejectsBlankRole() {
        assertThatThrownBy(() -> InterviewSessionFactory.start(" ", ExperienceLevel.JUNIOR,
                InterviewMode.TEXT, "Q", NOW)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void transitionsReturnNewInstancesWithSameId() {
        InterviewSession s = InterviewSessionFactory.start("Dev", ExperienceLevel.MID, InterviewMode.VOICE, "Q1", NOW);
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
    void evaluationScoresMustBeInRange() {
        assertThatThrownBy(() -> EvaluationFactory.create(11, 5, 5, 5, List.of(), List.of()))
                .isInstanceOf(DomainValidationException.class);
    }
}
