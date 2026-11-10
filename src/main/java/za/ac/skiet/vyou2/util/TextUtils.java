package za.ac.skiet.vyou2.util;

import java.util.Arrays;
import java.util.List;

public final class TextUtils {
    private TextUtils() {
    }

    public static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.strip().split("\\s+").length;
    }

    /** Lists are stored as newline-separated text so entities stay simple columns. */
    public static String joinLines(List<String> items) {
        return items == null ? "" : String.join("\n", items.stream().map(String::strip).filter(s -> !s.isEmpty()).toList());
    }

    public static List<String> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\n")).filter(s -> !s.isBlank()).toList();
    }
}
