package za.ac.skiet.vyou2.schemas;

import za.ac.skiet.vyou2.domain.ExperienceLevel;
import za.ac.skiet.vyou2.domain.InterviewMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartInterviewRequest(
        @NotBlank @Size(max = 120) String jobRole,
        @NotNull ExperienceLevel level,
        InterviewMode mode) {

    public InterviewMode modeOrDefault() {
        return mode == null ? InterviewMode.TEXT : mode;
    }
}
