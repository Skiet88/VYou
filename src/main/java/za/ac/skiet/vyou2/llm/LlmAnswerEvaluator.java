package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.exception.LlmResponseException;
import za.ac.skiet.vyou2.factory.EvaluationFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LlmAnswerEvaluator implements AnswerEvaluator {

    private final LlmClient llmClient;

    public LlmAnswerEvaluator(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public Evaluation evaluate(InterviewSession session, String question, String answer) {
        String raw = llmClient.complete(List.of(
                ChatMessage.system(PromptTemplates.EVALUATOR_SYSTEM),
                ChatMessage.user(PromptTemplates.evaluationRequest(
                        session.getJobRole(), session.getLevel().name(), question, answer))
        ), true);
        return toEvaluation(JsonResponseParser.parse(raw, EvaluationResult.class));
    }

    private static Evaluation toEvaluation(EvaluationResult r) {
        if (r.clarity() == null || r.technical() == null || r.confidence() == null || r.overall() == null) {
            throw new LlmResponseException("Evaluation JSON is missing one or more scores");
        }
        return EvaluationFactory.create(
                clamp(r.clarity()), clamp(r.technical()), clamp(r.confidence()), clamp(r.overall()),
                r.strengths() == null ? List.of() : r.strengths(),
                r.improvements() == null ? List.of() : r.improvements());
    }

    private static int clamp(int score) {
        return Math.max(EvaluationFactory.MIN_SCORE, Math.min(EvaluationFactory.MAX_SCORE, score));
    }
}
