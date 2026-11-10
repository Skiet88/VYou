package za.ac.skiet.vyou2.schemas;

import java.util.List;

public record EvaluationResponse(int clarity, int technical, int confidence, int overall,
                                 List<String> strengths, List<String> improvements) {
}
