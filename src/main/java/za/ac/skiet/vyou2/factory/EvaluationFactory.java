package za.ac.skiet.vyou2.factory;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.util.TextUtils;

import java.util.List;

import static za.ac.skiet.vyou2.util.Validation.requireNonNull;
import static za.ac.skiet.vyou2.util.Validation.requireRange;

public final class EvaluationFactory {

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 10;

    private EvaluationFactory() {
    }

    public static Evaluation create(int clarity, int technical, int confidence, int overall,
                                    List<String> strengths, List<String> improvements) {
        return new Evaluation(
                requireRange(clarity, MIN_SCORE, MAX_SCORE, "clarityScore"),
                requireRange(technical, MIN_SCORE, MAX_SCORE, "technicalScore"),
                requireRange(confidence, MIN_SCORE, MAX_SCORE, "confidenceScore"),
                requireRange(overall, MIN_SCORE, MAX_SCORE, "overallScore"),
                TextUtils.joinLines(requireNonNull(strengths, "strengths")),
                TextUtils.joinLines(requireNonNull(improvements, "improvements")));
    }
}
