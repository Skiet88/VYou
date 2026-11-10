package za.ac.skiet.vyou2.factory;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.InterviewTurn;
import za.ac.skiet.vyou2.exception.DomainValidationException;

import java.time.Instant;
import java.util.UUID;

import static za.ac.skiet.vyou2.util.Validation.requireNonNull;
import static za.ac.skiet.vyou2.util.Validation.requireText;

public final class InterviewTurnFactory {

    public static final int MAX_ANSWER_LENGTH = 10_000;

    private InterviewTurnFactory() {
    }

    public static InterviewTurn create(UUID sessionId, int sequenceNumber, String question, String answer,
                                       Evaluation evaluation, Instant now) {
        if (sequenceNumber < 1) {
            throw new DomainValidationException("sequenceNumber must be at least 1");
        }
        return new InterviewTurn(
                UUID.randomUUID(),
                requireNonNull(sessionId, "sessionId"),
                sequenceNumber,
                requireText(question, "question", InterviewSessionFactory.MAX_QUESTION_LENGTH),
                requireText(answer, "answer", MAX_ANSWER_LENGTH),
                requireNonNull(evaluation, "evaluation"),
                requireNonNull(now, "now"));
    }
}
