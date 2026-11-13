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

    @Column(nullable = false)
    private boolean emailVerified;

    /** SHA-256 hash of the current verification token, hex-encoded. Null once verified or never sent. */
    @Column(unique = true)
    private String verificationTokenHash;

    private Instant verificationTokenExpiresAt;

    /**
     * When the password last changed. A session caches this at login; if the database value has
     * since moved on, {@code ActiveAccountFilter} treats that session as stale and signs it out,
     * so a password reset also ends every other session for the account, not just this one's.
     */
    @Column(nullable = false)
    private Instant passwordChangedAt;

    /** SHA-256 hash of the current password-reset token, hex-encoded. Null once used or never requested. */
    @Column(unique = true)
    private String passwordResetTokenHash;

    private Instant passwordResetTokenExpiresAt;

    protected UserAccount() {
        // for JPA only
    }

    public UserAccount(UUID id, String email, String displayName, String passwordHash, UserRole role,
                       UserStatus status, Instant createdAt, Instant deletionScheduledAt, boolean emailVerified,
                       String verificationTokenHash, Instant verificationTokenExpiresAt, Instant passwordChangedAt,
                       String passwordResetTokenHash, Instant passwordResetTokenExpiresAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.deletionScheduledAt = deletionScheduledAt;
        this.emailVerified = emailVerified;
        this.verificationTokenHash = verificationTokenHash;
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
        this.passwordChangedAt = passwordChangedAt;
        this.passwordResetTokenHash = passwordResetTokenHash;
        this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
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

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public java.util.Optional<String> getVerificationTokenHash() {
        return java.util.Optional.ofNullable(verificationTokenHash);
    }

    public java.util.Optional<Instant> getVerificationTokenExpiresAt() {
        return java.util.Optional.ofNullable(verificationTokenExpiresAt);
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public java.util.Optional<String> getPasswordResetTokenHash() {
        return java.util.Optional.ofNullable(passwordResetTokenHash);
    }

    public java.util.Optional<Instant> getPasswordResetTokenExpiresAt() {
        return java.util.Optional.ofNullable(passwordResetTokenExpiresAt);
    }
}
