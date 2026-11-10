package za.ac.skiet.vyou2.schemas;

import java.time.Instant;

public record TurnResponse(int sequenceNumber, String question, String answer,
                           EvaluationResponse evaluation, Instant answeredAt) {
}
