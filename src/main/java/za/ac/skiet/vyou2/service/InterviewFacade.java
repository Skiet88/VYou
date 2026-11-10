package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Facade over sessions, turns, summaries, screening and the LLM. This is the only
 * interview service the API layer talks to.
 */
public interface InterviewFacade {

    InterviewSession start(UUID userId, String jobRole, ExperienceLevel level, InterviewMode mode);

    AnswerOutcome submitAnswer(UUID sessionId, String answer);

    /**
     * Completes the session and tries to generate the summary. Completion always succeeds even if
     * the LLM is unavailable; the summary is then empty and can be retried with {@link #summarise}.
     */
    CompletionOutcome complete(UUID sessionId);

    /** Returns the existing summary, or generates it if the session is completed and has none. */
    InterviewSummary summarise(UUID sessionId);

    SessionDetails details(UUID sessionId);

    List<InterviewSession> sessionsForUser(UUID userId);

    record CompletionOutcome(InterviewSession session, Optional<InterviewSummary> summary) {
    }
}
