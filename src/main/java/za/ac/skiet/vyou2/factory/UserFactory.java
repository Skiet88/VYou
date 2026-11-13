package za.ac.skiet.vyou2.factory;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.domain.UserRole;
import za.ac.skiet.vyou2.domain.UserStatus;
import za.ac.skiet.vyou2.exception.DomainValidationException;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import static za.ac.skiet.vyou2.util.Validation.requireNonNull;
import static za.ac.skiet.vyou2.util.Validation.requireText;

/**
 * The only sanctioned way to build or transition a {@link UserAccount}. Takes an already-hashed
 * password: hashing is an infrastructure concern (BCrypt via Spring Security), not a domain one.
 */
public final class UserFactory {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private UserFactory() {
    }

    /**
     * New self-registered users are always USER/ACTIVE and start unverified; promotion and
     * verification are separate, explicit transitions.
     */
    public static UserAccount register(String email, String displayName, String passwordHash, Instant now) {
        requireNonNull(now, "now");
        return new UserAccount(UUID.randomUUID(), normaliseEmail(email),
                requireText(displayName, "displayName", 80), requireText(passwordHash, "passwordHash", 200),
                UserRole.USER, UserStatus.ACTIVE, now, null, false, null, null, now, null, null);
    }

    public static UserAccount withRole(UserAccount user, UserRole role) {
        requireNonNull(user, "user");
        return copy(user).role(requireNonNull(role, "role")).build();
    }

    /** Suspend or reinstate. Not for deletion: use scheduleDeletion / cancelDeletion. */
    public static UserAccount withStatus(UserAccount user, UserStatus status) {
        requireNonNull(user, "user");
        if (requireNonNull(status, "status") == UserStatus.PENDING_DELETION || user.isPendingDeletion()) {
            throw new DomainValidationException("Use scheduleDeletion/cancelDeletion for account deletion");
        }
        return copy(user).status(status).deletionScheduledAt(null).build();
    }

    /** The user asked to delete their account: it is purged at {@code purgeAt} unless cancelled first. */
    public static UserAccount scheduleDeletion(UserAccount user, Instant purgeAt) {
        requireNonNull(user, "user");
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DomainValidationException("Only an active account can be scheduled for deletion");
        }
        return copy(user).status(UserStatus.PENDING_DELETION).deletionScheduledAt(requireNonNull(purgeAt, "purgeAt")).build();
    }

    public static UserAccount cancelDeletion(UserAccount user) {
        requireNonNull(user, "user");
        if (!user.isPendingDeletion()) {
            throw new DomainValidationException("This account is not scheduled for deletion");
        }
        return copy(user).status(UserStatus.ACTIVE).deletionScheduledAt(null).build();
    }

    /** Replaces any existing verification token (e.g. a fresh signup, or a resend request). */
    public static UserAccount withVerificationToken(UserAccount user, String tokenHash, Instant expiresAt) {
        requireNonNull(user, "user");
        return copy(user)
                .verificationTokenHash(requireText(tokenHash, "tokenHash", 100))
                .verificationTokenExpiresAt(requireNonNull(expiresAt, "expiresAt"))
                .build();
    }

    /** Marks the account verified and clears the token, so it can never be reused. */
    public static UserAccount verifyEmail(UserAccount user) {
        requireNonNull(user, "user");
        if (user.isEmailVerified()) {
            throw new DomainValidationException("This account's email is already verified");
        }
        return copy(user).emailVerified(true).verificationTokenHash(null).verificationTokenExpiresAt(null).build();
    }

    /** Replaces any existing password-reset token (e.g. a fresh "forgot password" request). */
    public static UserAccount withPasswordResetToken(UserAccount user, String tokenHash, Instant expiresAt) {
        requireNonNull(user, "user");
        return copy(user)
                .passwordResetTokenHash(requireText(tokenHash, "tokenHash", 100))
                .passwordResetTokenExpiresAt(requireNonNull(expiresAt, "expiresAt"))
                .build();
    }

    /**
     * Sets a new password from a successful reset, clears the token so it can never be reused,
     * and records when it changed - {@code ActiveAccountFilter} uses that to sign out every other
     * session for the account, not just the one performing the reset.
     */
    public static UserAccount resetPassword(UserAccount user, String newPasswordHash, Instant now) {
        requireNonNull(user, "user");
        return copy(user)
                .passwordHash(requireText(newPasswordHash, "newPasswordHash", 200))
                .passwordChangedAt(requireNonNull(now, "now"))
                .passwordResetTokenHash(null)
                .passwordResetTokenExpiresAt(null)
                .build();
    }

    public static String normaliseEmail(String email) {
        String normalised = requireText(email, "email", 254).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalised).matches()) {
            throw new DomainValidationException("email is not a valid address");
        }
        return normalised;
    }

    /** Starts from every field of an existing account, so a transition only has to state what changes. */
    private static Builder copy(UserAccount user) {
        return new Builder(user);
    }

    private static final class Builder {
        private final UUID id;
        private final String email;
        private final String displayName;
        private String passwordHash;
        private UserRole role;
        private UserStatus status;
        private final Instant createdAt;
        private Instant deletionScheduledAt;
        private boolean emailVerified;
        private String verificationTokenHash;
        private Instant verificationTokenExpiresAt;
        private Instant passwordChangedAt;
        private String passwordResetTokenHash;
        private Instant passwordResetTokenExpiresAt;

        Builder(UserAccount user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.displayName = user.getDisplayName();
            this.passwordHash = user.getPasswordHash();
            this.role = user.getRole();
            this.status = user.getStatus();
            this.createdAt = user.getCreatedAt();
            this.deletionScheduledAt = user.getDeletionScheduledAt().orElse(null);
            this.emailVerified = user.isEmailVerified();
            this.verificationTokenHash = user.getVerificationTokenHash().orElse(null);
            this.verificationTokenExpiresAt = user.getVerificationTokenExpiresAt().orElse(null);
            this.passwordChangedAt = user.getPasswordChangedAt();
            this.passwordResetTokenHash = user.getPasswordResetTokenHash().orElse(null);
            this.passwordResetTokenExpiresAt = user.getPasswordResetTokenExpiresAt().orElse(null);
        }

        Builder passwordHash(String value) {
            this.passwordHash = value;
            return this;
        }

        Builder role(UserRole value) {
            this.role = value;
            return this;
        }

        Builder status(UserStatus value) {
            this.status = value;
            return this;
        }

        Builder deletionScheduledAt(Instant value) {
            this.deletionScheduledAt = value;
            return this;
        }

        Builder emailVerified(boolean value) {
            this.emailVerified = value;
            return this;
        }

        Builder verificationTokenHash(String value) {
            this.verificationTokenHash = value;
            return this;
        }

        Builder verificationTokenExpiresAt(Instant value) {
            this.verificationTokenExpiresAt = value;
            return this;
        }

        Builder passwordChangedAt(Instant value) {
            this.passwordChangedAt = value;
            return this;
        }

        Builder passwordResetTokenHash(String value) {
            this.passwordResetTokenHash = value;
            return this;
        }

        Builder passwordResetTokenExpiresAt(Instant value) {
            this.passwordResetTokenExpiresAt = value;
            return this;
        }

        UserAccount build() {
            return new UserAccount(id, email, displayName, passwordHash, role, status, createdAt, deletionScheduledAt,
                    emailVerified, verificationTokenHash, verificationTokenExpiresAt, passwordChangedAt,
                    passwordResetTokenHash, passwordResetTokenExpiresAt);
        }
    }
}
