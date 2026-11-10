package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewTurn;

import java.util.List;
import java.util.UUID;

public interface InterviewTurnService {

    InterviewTurn record(InterviewTurn turn);

    List<InterviewTurn> forSession(UUID sessionId);
}
