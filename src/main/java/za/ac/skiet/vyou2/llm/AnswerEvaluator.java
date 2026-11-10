package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.domain.Evaluation;
import za.ac.skiet.vyou2.domain.InterviewSession;

public interface AnswerEvaluator {
    Evaluation evaluate(InterviewSession session, String question, String answer);
}
