package za.ac.skiet.vyou2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable interview session. Create through {@code InterviewSessionFactory};
 * state changes return a new instance with the same id.
 */
@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    @Id
    private UUID id;

    /** Optimistic lock: a concurrent update of the same session fails instead of overwriting. */
    @Version
    private Long version;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackMode feedbackMode;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private String jobRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperienceLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

    /** The question currently awaiting an answer; null when completed. */
    @Column(length = 2000)
    private String pendingQuestion;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant endedAt;

    protected InterviewSession() {
        // for JPA only
    }

    public InterviewSession(UUID id, Long version, UUID userId, FeedbackMode feedbackMode, int totalQuestions,
                            String jobRole, ExperienceLevel level, InterviewMode mode,
                            InterviewStatus status, String pendingQuestion, Instant startedAt, Instant endedAt) {
        this.id = id;
        this.version = version;
        this.userId = userId;
        this.feedbackMode = feedbackMode;
        this.totalQuestions = totalQuestions;
        this.jobRole = jobRole;
        this.level = level;
        this.mode = mode;
        this.status = status;
        this.pendingQuestion = pendingQuestion;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public UUID getUserId() {
        return userId;
    }

    public FeedbackMode getFeedbackMode() {
        return feedbackMode;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public String getJobRole() {
        return jobRole;
    }

    public ExperienceLevel getLevel() {
        return level;
    }

    public InterviewMode getMode() {
        return mode;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public Optional<String> getPendingQuestion() {
        return Optional.ofNullable(pendingQuestion);
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Optional<Instant> getEndedAt() {
        return Optional.ofNullable(endedAt);
    }

    public boolean isInProgress() {
        return status == InterviewStatus.IN_PROGRESS;
    }
}
