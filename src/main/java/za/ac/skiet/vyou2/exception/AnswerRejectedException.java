package za.ac.skiet.vyou2.exception;

import java.util.List;

/** Thrown when screening rejects an answer before it reaches the LLM. */
public class AnswerRejectedException extends RuntimeException {
    private final List<String> reasons;

    public AnswerRejectedException(List<String> reasons) {
        super("Answer rejected: " + String.join("; ", reasons));
        this.reasons = List.copyOf(reasons);
    }

    public List<String> getReasons() {
        return reasons;
    }
}
