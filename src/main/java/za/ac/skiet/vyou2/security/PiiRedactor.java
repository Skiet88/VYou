package za.ac.skiet.vyou2.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Strips personal identifiers from text before it is sent to a third-party LLM. All patterns are
 * linear (no nested quantifiers over the same class) so they cannot be driven into catastrophic
 * backtracking (ReDoS) regardless of input.
 *
 * The original, unredacted text is still what gets stored and shown back to the user; only the
 * copy sent to the model is redacted.
 */
@Component
public class PiiRedactor {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]{1,64}@[\\w-]{1,64}(?:\\.[\\w-]{1,64}){1,4}");
    // South African ID: 13 digits. Matched as a whole number token to avoid catching phone numbers.
    private static final Pattern SA_ID = Pattern.compile("(?<!\\d)\\d{13}(?!\\d)");
    // Phone numbers: optional +, then 9-15 digits/spaces/dashes, at least 9 digits total.
    private static final Pattern PHONE = Pattern.compile("(?<!\\w)\\+?\\d(?:[\\d \\-]{7,14}\\d)(?!\\w)");
    private static final Pattern URL = Pattern.compile("(?:https?://|www\\.)[^\\s]{1,256}");

    public String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = EMAIL.matcher(text).replaceAll("[redacted-email]");
        result = URL.matcher(result).replaceAll("[redacted-url]");
        result = SA_ID.matcher(result).replaceAll("[redacted-id-number]");
        result = PHONE.matcher(result).replaceAll("[redacted-phone]");
        return result;
    }
}
