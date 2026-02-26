package za.ac.skiet.vyou.service.InterviewTurn;


import org.springframework.stereotype.Service;
import za.ac.skiet.vyou.domain.InterviewTurn;
import za.ac.skiet.vyou.service.IService;

import java.util.List;
import java.util.UUID;

public interface IInterviewTurnService extends IService<InterviewTurn, UUID> {
    List<InterviewTurn> getAll();
    List<InterviewTurn> findByInterviewSessionId(UUID sessionId);
    InterviewTurn submitAnswer(UUID turnId, String userAnswer);
}

