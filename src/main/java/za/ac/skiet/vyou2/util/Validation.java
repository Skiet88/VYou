package za.ac.skiet.vyou2.util;

import za.ac.skiet.vyou2.exception.DomainValidationException;

/** Guard clauses shared by the validating factories. */
public final class Validation {
    private Validation() {
    }

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new DomainValidationException(field + " is required");
        }
        return value;
    }

    public static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field + " must not be blank");
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new DomainValidationException(field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    public static int requireRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new DomainValidationException(field + " must be between " + min + " and " + max);
        }
        return value;
    }
}
