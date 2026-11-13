package za.ac.skiet.vyou2.schemas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Password length is capped at 72 bytes: BCrypt silently truncates anything longer. */
public record PasswordResetConfirmation(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
}
