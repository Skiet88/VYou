package za.ac.skiet.vyou2.schemas;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Password length is capped at 72 bytes: BCrypt silently truncates anything longer. */
public record SignupRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
