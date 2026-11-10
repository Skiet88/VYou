package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewTurn;
import za.ac.skiet.vyou2.exception.LlmResponseException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LlmSummaryGenerator implements SummaryGenerator {

    private final LlmClient llmClient;

    public LlmSummaryGenerator(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public SummaryDraft summarise(InterviewSession session, List<InterviewTurn> turns) {
        StringBuilder transcript = new StringBuilder()
                .append("Role: ").append(session.getJobRole())
                .append("\nLevel: ").append(session.getLevel()).append('\n');
        for (InterviewTurn t : turns) {
            Evaluation e = t.getEvaluation();
            transcript.append("\nQ").append(t.getSequenceNumber()).append(": ").append(t.getQuestion())
                    .append("\nAnswer: ").append(t.getAnswer())
                    .append("\nScores: clarity ").append(e.getClarityScore())
                    .append(", technical ").append(e.getTechnicalScore())
                    .append(", confidence ").append(e.getConfidenceScore())
                    .append(", overall ").append(e.getOverallScore()).append('\n');
        }
        String raw = llmClient.complete(List.of(
                ChatMessage.system(PromptTemplates.SUMMARY_SYSTEM),
                ChatMessage.user(transcript.toString())), true);
        SummaryDraft draft = JsonResponseParser.parse(raw, SummaryDraft.class);
        if (draft.feedback() == null || draft.feedback().isBlank()) {
            throw new LlmResponseException("Summary JSON has no feedback");
        }
        return new SummaryDraft(draft.feedback(),
                draft.topStrengths() == null ? List.of() : draft.topStrengths(),
                draft.focusAreas() == null ? List.of() : draft.focusAreas());
    }
}
