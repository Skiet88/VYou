package za.ac.skiet.vyou2.schemas;

import za.ac.skiet.vyou2.domain.UserRole;
import za.ac.skiet.vyou2.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

/** What an admin sees about a user: account details and aggregate numbers, never interview content. */
public record AdminUserResponse(UUID id, String email, String displayName, UserRole role, UserStatus status,
                                Instant createdAt, int interviewsTotal, int interviewsCompleted, Double averageOverall) {
}
