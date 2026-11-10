package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewSession;

import java.util.UUID;

/**
 * Facade over sessions, turns, screening and the LLM. This is the only service
 * the API layer talks to.
 */
public interface InterviewFacade {

    InterviewSession start(String jobRole, ExperienceLevel level, InterviewMode mode);

    AnswerOutcome submitAnswer(UUID sessionId, String answer);

    InterviewSession complete(UUID sessionId);

    SessionDetails details(UUID sessionId);
}
