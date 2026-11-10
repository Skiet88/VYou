package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.FeedbackMode;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewStatus;
import za.ac.skiet.vyou2.domain.InterviewSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Facade over sessions, turns, summaries, screening and the LLM. This is the only
 * interview service the API layer talks to.
 *
 * Every method that takes a sessionId also takes the caller's userId and enforces ownership:
 * a session that exists but belongs to someone else is reported as {@code ResourceNotFoundException}
 * (404), never as "forbidden" (403), so a caller cannot use the response to tell the difference
 * between "not yours" and "does not exist" (object-level authorization / anti-IDOR).
 */
public interface InterviewFacade {

    InterviewSession start(UUID userId, String jobRole, ExperienceLevel level, InterviewMode mode,
                           FeedbackMode feedbackMode);

    AnswerOutcome submitAnswer(UUID userId, UUID sessionId, String answer);

    /**
     * Completes the session and tries to generate the summary. Completion always succeeds even if
     * the LLM is unavailable; the summary is then empty and can be retried with {@link #summarise}.
     */
    CompletionOutcome complete(UUID userId, UUID sessionId);

    /** Returns the existing summary, or generates it if the session is completed and has none. */
    InterviewSummary summarise(UUID userId, UUID sessionId);

    SessionDetails details(UUID userId, UUID sessionId);

    /** One page of the caller's own interviews, newest first; {@code status} null means all. */
    PageResult<SessionOverview> sessionsForUser(UUID userId, InterviewStatus status, int page, int size);

    record CompletionOutcome(InterviewSession session, int questionsAnswered, Optional<InterviewSummary> summary) {
    }

    /** {@code averageOverall} is empty until the interview has a summary. */
    record SessionOverview(InterviewSession session, int questionsAnswered, Optional<Double> averageOverall) {
    }
}
