package za.ac.skiet.vyou2.schemas;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.FeedbackMode;
import za.ac.skiet.vyou2.domain.InterviewMode;
import za.ac.skiet.vyou2.domain.InterviewStatus;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID id, String jobRole, ExperienceLevel level, InterviewMode mode,
                              InterviewStatus status, FeedbackMode feedbackMode, int totalQuestions,
                              int questionsAnswered, String currentQuestion, Instant startedAt, Instant endedAt) {
}
