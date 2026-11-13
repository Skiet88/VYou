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
        return new UserAccount(UUID.randomUUID(), normaliseEmail(email),
                requireText(displayName, "displayName", 80), requireText(passwordHash, "passwordHash", 200),
                UserRole.USER, UserStatus.ACTIVE, requireNonNull(now, "now"), null, false, null, null);
    }

    public static UserAccount withRole(UserAccount user, UserRole role) {
        requireNonNull(user, "user");
        return new UserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
                requireNonNull(role, "role"), user.getStatus(), user.getCreatedAt(), user.getDeletionScheduledAt().orElse(null),
                user.isEmailVerified(), user.getVerificationTokenHash().orElse(null),
                user.getVerificationTokenExpiresAt().orElse(null));
    }

    /** Suspend or reinstate. Not for deletion: use scheduleDeletion / cancelDeletion. */
    public static UserAccount withStatus(UserAccount user, UserStatus status) {
        requireNonNull(user, "user");
        if (requireNonNull(status, "status") == UserStatus.PENDING_DELETION || user.isPendingDeletion()) {
            throw new DomainValidationException("Use scheduleDeletion/cancelDeletion for account deletion");
        }
        return new UserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
                user.getRole(), status, user.getCreatedAt(), null, user.isEmailVerified(),
                user.getVerificationTokenHash().orElse(null), user.getVerificationTokenExpiresAt().orElse(null));
    }

    /** The user asked to delete their account: it is purged at {@code purgeAt} unless cancelled first. */
    public static UserAccount scheduleDeletion(UserAccount user, Instant purgeAt) {
        requireNonNull(user, "user");
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DomainValidationException("Only an active account can be scheduled for deletion");
        }
        return new UserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
                user.getRole(), UserStatus.PENDING_DELETION, user.getCreatedAt(), requireNonNull(purgeAt, "purgeAt"),
                user.isEmailVerified(), user.getVerificationTokenHash().orElse(null),
                user.getVerificationTokenExpiresAt().orElse(null));
    }

    public static UserAccount cancelDeletion(UserAccount user) {
        requireNonNull(user, "user");
        if (!user.isPendingDeletion()) {
            throw new DomainValidationException("This account is not scheduled for deletion");
        }
        return new UserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
                user.getRole(), UserStatus.ACTIVE, user.getCreatedAt(), null, user.isEmailVerified(),
                user.getVerificationTokenHash().orElse(null), user.getVerificationTokenExpiresAt().orElse(null));
    }

    /** Replaces any existing verification token (e.g. a fresh signup, or a resend request). */
    public static UserAccount withVerificationToken(UserAccount user, String tokenHash, Instant expiresAt) {
        requireNonNull(user, "user");
        return new UserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
                user.getRole(), user.getStatus(), user.getCreatedAt(), user.getDeletionScheduledAt().orElse(null),
                user.isEmailVerified(), requireText(tokenHash, "tokenHash", 100), requireNonNull(expiresAt, "expiresAt"));
    }

    /** Marks the account verified and clears the token, so it can never be reused. */
    public static UserAccount verifyEmail(UserAccount user) {
        requireNonNull(user, "user");
        if (user.isEmailVerified()) {
            throw new DomainValidationException("This account's email is already verified");
        }
        return new UserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
                user.getRole(), user.getStatus(), user.getCreatedAt(), user.getDeletionScheduledAt().orElse(null),
                true, null, null);
    }

    public static String normaliseEmail(String email) {
        String normalised = requireText(email, "email", 254).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalised).matches()) {
            throw new DomainValidationException("email is not a valid address");
        }
        return normalised;
    }
}
