package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewTurn;

import java.util.List;

public interface SummaryGenerator {
    SummaryDraft summarise(InterviewSession session, List<InterviewTurn> turns);
}
