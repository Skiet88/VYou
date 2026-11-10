package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewTurn;

/** Result of submitting an answer: the stored turn and the session (with its next question, if any). */
public record AnswerOutcome(InterviewTurn turn, InterviewSession session) {
}
