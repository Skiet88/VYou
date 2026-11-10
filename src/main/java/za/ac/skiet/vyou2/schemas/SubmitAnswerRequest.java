package za.ac.skiet.vyou2.schemas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitAnswerRequest(@NotBlank @Size(max = 10000) String answer) {
}
