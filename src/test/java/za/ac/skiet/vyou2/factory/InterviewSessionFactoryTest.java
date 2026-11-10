package za.ac.skiet.vyou2.factory;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.FeedbackMode;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewStatus;
import za.ac.skiet.vyou2.domain.UserRole;
import za.ac.skiet.vyou2.domain.UserStatus;
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
        InterviewSession s = InterviewSessionFactory.start(USER, FeedbackMode.INSTANT, 5, "  Java Developer ",
                ExperienceLevel.JUNIOR, InterviewMode.TEXT, "Tell me about yourself", NOW);
        assertThat(s.getJobRole()).isEqualTo("Java Developer");
        assertThat(s.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(s.getPendingQuestion()).contains("Tell me about yourself");
        assertThat(s.getTotalQuestions()).isEqualTo(5);
        assertThat(s.isOwnedBy(USER)).isTrue();
        assertThat(s.isOwnedBy(UUID.randomUUID())).isFalse();
    }

    @Test
    void rejectsBlankRole() {
        assertThatThrownBy(() -> InterviewSessionFactory.start(USER, FeedbackMode.INSTANT, 5, " ",
                ExperienceLevel.JUNIOR, InterviewMode.TEXT, "Q", NOW)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsNonPositiveQuestionCount() {
        assertThatThrownBy(() -> InterviewSessionFactory.start(USER, FeedbackMode.INSTANT, 0, "Dev",
                ExperienceLevel.JUNIOR, InterviewMode.TEXT, "Q", NOW)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void transitionsReturnNewInstancesWithSameId() {
        InterviewSession s = InterviewSessionFactory.start(USER, FeedbackMode.END_OF_INTERVIEW, 5, "Dev",
                ExperienceLevel.MID, InterviewMode.VOICE, "Q1", NOW);
        InterviewSession next = InterviewSessionFactory.withNextQuestion(s, "Q2");
        InterviewSession done = InterviewSessionFactory.complete(next, NOW.plusSeconds(60));

        assertThat(next).isNotSameAs(s);
        assertThat(next.getId()).isEqualTo(s.getId());
        assertThat(next.getFeedbackMode()).isEqualTo(FeedbackMode.END_OF_INTERVIEW);
        assertThat(s.getPendingQuestion()).contains("Q1");
        assertThat(done.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(done.getPendingQuestion()).isEmpty();
        assertThatThrownBy(() -> InterviewSessionFactory.complete(done, NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void startRequiresUser() {
        assertThatThrownBy(() -> InterviewSessionFactory.start(null, FeedbackMode.INSTANT, 5, "Dev",
                ExperienceLevel.JUNIOR, InterviewMode.TEXT, "Q", NOW)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void userRegistrationNormalisesEmailAndDefaultsRoleAndStatus() {
        var user = UserFactory.register(" Thabo@Example.COM ", "Thabo", "bcrypt-hash", NOW);
        assertThat(user.getEmail()).isEqualTo("thabo@example.com");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThatThrownBy(() -> UserFactory.register("not-an-email", "Thabo", "hash", NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void userRoleAndStatusTransitionsPreserveEverythingElse() {
        var user = UserFactory.register("a@example.com", "A", "hash", NOW);
        var admin = UserFactory.withRole(user, UserRole.ADMIN);
        var suspended = UserFactory.withStatus(user, UserStatus.SUSPENDED);
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getId()).isEqualTo(user.getId());
        assertThat(suspended.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(suspended.isActive()).isFalse();
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
