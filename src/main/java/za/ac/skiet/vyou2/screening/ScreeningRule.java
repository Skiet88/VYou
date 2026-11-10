package za.ac.skiet.vyou2.screening;

/**
 * Strategy: one cheap, local check run on an answer before it is sent to the LLM,
 * so obviously unusable answers don't spend the free-tier request quota.
 */
public interface ScreeningRule {
    ScreeningResult check(String question, String answer);
}
