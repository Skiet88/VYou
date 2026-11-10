package za.ac.skiet.vyou2.screening;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Runs every {@link ScreeningRule} bean and collects all rejection reasons. */
@Component
public class RuleBasedAnswerScreener implements AnswerScreener {

    private final List<ScreeningRule> rules;

    public RuleBasedAnswerScreener(List<ScreeningRule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public ScreeningResult screen(String question, String answer) {
        List<String> reasons = new ArrayList<>();
        for (ScreeningRule rule : rules) {
            ScreeningResult result = rule.check(question, answer);
            if (!result.passed()) {
                reasons.addAll(result.reasons());
            }
        }
        return reasons.isEmpty() ? ScreeningResult.pass() : new ScreeningResult(false, reasons);
    }
}
