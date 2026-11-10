package za.ac.skiet.vyou2.factory;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.exception.DomainValidationException;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import static za.ac.skiet.vyou2.util.Validation.requireNonNull;
import static za.ac.skiet.vyou2.util.Validation.requireText;

public final class UserFactory {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private UserFactory() {
    }

    public static UserAccount create(String email, String displayName, Instant now) {
        String normalisedEmail = requireText(email, "email", 254).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalisedEmail).matches()) {
            throw new DomainValidationException("email is not a valid address");
        }
        return new UserAccount(UUID.randomUUID(), normalisedEmail,
                requireText(displayName, "displayName", 80), requireNonNull(now, "now"));
    }
}
