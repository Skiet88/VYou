package za.ac.skiet.vyou2.schemas;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.FeedbackMode;
import za.ac.skiet.vyou2.domain.InterviewStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * A row in the interview list. Deliberately smaller than SessionResponse: no question text, and no
 * answer or feedback content, only what a list needs.
 */
public record InterviewListItemResponse(UUID id, String jobRole, ExperienceLevel level, InterviewStatus status,
                                        FeedbackMode feedbackMode, int totalQuestions, int questionsAnswered,
                                        Double averageOverall, Instant startedAt, Instant endedAt) {
}
