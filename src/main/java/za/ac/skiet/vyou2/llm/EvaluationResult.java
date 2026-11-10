package za.ac.skiet.vyou2.llm;

import java.util.List;

/** Raw shape of the JSON the evaluation prompt asks the model to return. */
public record EvaluationResult(Integer clarity, Integer technical, Integer confidence, Integer overall,
                               List<String> strengths, List<String> improvements) {
}
