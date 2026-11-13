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

    /** New self-registered users are always USER/ACTIVE; promotion is a separate, explicit transition. */
    public static UserAccount register(String email, String displayName, String passwordHash, Instant now) {
        return new UserAccount(UUID.randomUUID(), normaliseEmail(email),
                requireText(displayName, "displayName", 80), requireText(passwordHash, "passwordHash", 200),
                UserRole.USER, UserStatus.ACTIVE, requireNonNull(now, "now"), null);
    }

    public static UserAccount withRole(UserAccount user, UserRole role) {
        requireNonNull(user, "user");
        return new UserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
                requireNonNull(role, "role"), user.getStatus(), user.getCreatedAt(), user.getDeletionScheduledAt().orElse(null));
    }

    /** Suspend or reinstate. Not for deletion: use scheduleDeletion / cancelDeletion. */
    public static UserAccount withStatus(UserAccount user, UserStatus status) {
        requireNonNull(user, "user");
        if (requireNonNull(status, "status") == UserStatus.PENDING_DELETION || user.isPendingDeletion()) {
            throw new DomainValidationException("Use scheduleDeletion/cancelDeletion for account deletion");
        }
        return new UserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
                user.getRole(), status, user.getCreatedAt(), null);
    }

    /** The user asked to delete their account: it is purged at {@code purgeAt} unless cancelled first. */
    public static UserAccount scheduleDeletion(UserAccount user, Instant purgeAt) {
        requireNonNull(user, "user");
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DomainValidationException("Only an active account can be scheduled for deletion");
        }
        return new UserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
                user.getRole(), UserStatus.PENDING_DELETION, user.getCreatedAt(), requireNonNull(purgeAt, "purgeAt"));
    }

    public static UserAccount cancelDeletion(UserAccount user) {
        requireNonNull(user, "user");
        if (!user.isPendingDeletion()) {
            throw new DomainValidationException("This account is not scheduled for deletion");
        }
        return new UserAccount(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash(),
                user.getRole(), UserStatus.ACTIVE, user.getCreatedAt(), null);
    }

    public static String normaliseEmail(String email) {
        String normalised = requireText(email, "email", 254).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalised).matches()) {
            throw new DomainValidationException("email is not a valid address");
        }
        return normalised;
    }
}
