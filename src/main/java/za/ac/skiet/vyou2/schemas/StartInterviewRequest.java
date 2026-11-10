package za.ac.skiet.vyou2.schemas;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.FeedbackMode;
import za.ac.skiet.vyou2.domain.InterviewMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * No userId field: the session is always started for the authenticated caller (prevents mass
 * assignment of ownership). Question count is not client-settable, for the same reason plus
 * cost control over the AI quota.
 */
public record StartInterviewRequest(
        @NotBlank @Size(max = 120) String jobRole,
        @NotNull ExperienceLevel level,
        InterviewMode mode,
        FeedbackMode feedbackMode) {

    public InterviewMode modeOrDefault() {
        return mode == null ? InterviewMode.TEXT : mode;
    }

    public FeedbackMode feedbackModeOrDefault() {
        return feedbackMode == null ? FeedbackMode.INSTANT : feedbackMode;
    }
}
