package za.ac.skiet.vyou2.factory;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewStatus;
import za.ac.skiet.vyou2.exception.DomainValidationException;

import java.time.Instant;
import java.util.UUID;

import static za.ac.skiet.vyou2.util.Validation.requireNonNull;
import static za.ac.skiet.vyou2.util.Validation.requireText;

/** The only sanctioned way to build or transition an {@link InterviewSession}. */
public final class InterviewSessionFactory {

    public static final int MAX_ROLE_LENGTH = 120;
    public static final int MAX_QUESTION_LENGTH = 2000;

    private InterviewSessionFactory() {
    }

    public static InterviewSession start(UUID userId, String jobRole, ExperienceLevel level, InterviewMode mode,
                                         String firstQuestion, Instant now) {
        return new InterviewSession(
                UUID.randomUUID(),
                null,
                requireNonNull(userId, "userId"),
                requireText(jobRole, "jobRole", MAX_ROLE_LENGTH),
                requireNonNull(level, "level"),
                requireNonNull(mode, "mode"),
                InterviewStatus.IN_PROGRESS,
                requireText(firstQuestion, "firstQuestion", MAX_QUESTION_LENGTH),
                requireNonNull(now, "now"),
                null);
    }

    public static InterviewSession withNextQuestion(InterviewSession session, String nextQuestion) {
        requireInProgress(session);
        return new InterviewSession(session.getId(), session.getVersion(), session.getUserId(), session.getJobRole(), session.getLevel(), session.getMode(),
                session.getStatus(), requireText(nextQuestion, "nextQuestion", MAX_QUESTION_LENGTH),
                session.getStartedAt(), null);
    }

    public static InterviewSession complete(InterviewSession session, Instant now) {
        requireInProgress(session);
        return new InterviewSession(session.getId(), session.getVersion(), session.getUserId(), session.getJobRole(), session.getLevel(), session.getMode(),
                InterviewStatus.COMPLETED, null, session.getStartedAt(), requireNonNull(now, "now"));
    }

    private static void requireInProgress(InterviewSession session) {
        requireNonNull(session, "session");
        if (!session.isInProgress()) {
            throw new DomainValidationException("Interview session " + session.getId() + " is already completed");
        }
    }
}
