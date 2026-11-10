package za.ac.skiet.vyou2.schemas;

import za.ac.skiet.vyou2.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, UserRole role, Instant createdAt) {
}
