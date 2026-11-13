package za.ac.skiet.vyou2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Immutable user. Create through {@code UserFactory}; role/status changes return a new instance. */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String displayName;

    /** BCrypt hash. Never logged, never returned by the API. */
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    /** When a PENDING_DELETION account will be permanently deleted; null otherwise. */
    private Instant deletionScheduledAt;

    protected UserAccount() {
        // for JPA only
    }

    public UserAccount(UUID id, String email, String displayName, String passwordHash, UserRole role,
                       UserStatus status, Instant createdAt, Instant deletionScheduledAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.deletionScheduledAt = deletionScheduledAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public java.util.Optional<Instant> getDeletionScheduledAt() {
        return java.util.Optional.ofNullable(deletionScheduledAt);
    }

    public boolean isPendingDeletion() {
        return status == UserStatus.PENDING_DELETION;
    }
}
