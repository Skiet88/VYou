package za.ac.skiet.vyou2.factory;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.InterviewSummary;
import za.ac.skiet.vyou2.domain.InterviewTurn;
import za.ac.skiet.vyou2.exception.DomainValidationException;
import za.ac.skiet.vyou2.util.TextUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.ToIntFunction;

import static za.ac.skiet.vyou2.util.Validation.requireNonNull;
import static za.ac.skiet.vyou2.util.Validation.requireText;

public final class InterviewSummaryFactory {

    private InterviewSummaryFactory() {
    }

    public static InterviewSummary create(UUID sessionId, List<InterviewTurn> turns, String feedback,
                                          List<String> topStrengths, List<String> focusAreas, Instant now) {
        requireNonNull(turns, "turns");
        if (turns.isEmpty()) {
            throw new DomainValidationException("Cannot summarise an interview with no answered questions");
        }
        return new InterviewSummary(
                UUID.randomUUID(),
                requireNonNull(sessionId, "sessionId"),
                turns.size(),
                average(turns, Evaluation::getClarityScore),
                average(turns, Evaluation::getTechnicalScore),
                average(turns, Evaluation::getConfidenceScore),
                average(turns, Evaluation::getOverallScore),
                requireText(feedback, "feedback", 4000),
                TextUtils.joinLines(requireNonNull(topStrengths, "topStrengths")),
                TextUtils.joinLines(requireNonNull(focusAreas, "focusAreas")),
                requireNonNull(now, "now"));
    }

    private static double average(List<InterviewTurn> turns, ToIntFunction<Evaluation> score) {
        double avg = turns.stream().mapToInt(t -> score.applyAsInt(t.getEvaluation())).average().orElse(0);
        return Math.round(avg * 10) / 10.0;
    }
}
