package za.ac.skiet.vyou2.schemas;

import za.ac.skiet.vyou2.domain.InterviewStatus;

/** {@code evaluation} inside turn is null while feedback is withheld until the interview ends. */
public record AnswerResponse(TurnResponse turn, String nextQuestion, InterviewStatus sessionStatus) {
}
