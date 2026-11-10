package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.exception.LlmResponseException;
import za.ac.skiet.vyou2.factory.EvaluationFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LlmAnswerEvaluator implements AnswerEvaluator {

    private final LlmClient llmClient;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
        return toEvaluation(parse(raw));
    }

    EvaluationResult parse(String raw) {
        String json = stripCodeFence(raw);
        try {
            return mapper.readValue(json, EvaluationResult.class);
        } catch (JsonProcessingException ex) {
            throw new LlmResponseException("Evaluation was not valid JSON", ex);
        }
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

    /** Some models wrap JSON in markdown fences even in JSON mode. */
    private static String stripCodeFence(String raw) {
        String s = raw.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                s = s.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return s;
    }
}
