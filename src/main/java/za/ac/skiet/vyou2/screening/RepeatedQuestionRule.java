package za.ac.skiet.vyou2.screening;

import org.springframework.stereotype.Component;

import java.util.Locale;

/** Rejects answers that just echo the question back. */
@Component
public class RepeatedQuestionRule implements ScreeningRule {

    @Override
    public ScreeningResult check(String question, String answer) {
        if (question == null || answer == null) {
            return ScreeningResult.pass();
        }
        String q = normalise(question);
        String a = normalise(answer);
        return !a.isEmpty() && a.equals(q)
                ? ScreeningResult.reject("Answer repeats the question")
                : ScreeningResult.pass();
    }

    private static String normalise(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").strip();
    }
}
