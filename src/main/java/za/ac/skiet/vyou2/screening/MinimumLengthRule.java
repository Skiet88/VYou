package za.ac.skiet.vyou2.screening;

import za.ac.skiet.vyou2.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MinimumLengthRule implements ScreeningRule {

    private final int minWords;

    public MinimumLengthRule(@Value("${vyou.screening.min-words:5}") int minWords) {
        this.minWords = minWords;
    }

    @Override
    public ScreeningResult check(String question, String answer) {
        int words = TextUtils.wordCount(answer);
        return words >= minWords
                ? ScreeningResult.pass()
                : ScreeningResult.reject("Answer is too short (" + words + " words, minimum " + minWords + ")");
    }
}
