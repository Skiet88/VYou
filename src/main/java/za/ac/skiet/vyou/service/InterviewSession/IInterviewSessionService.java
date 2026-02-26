package za.ac.skiet.vyou.service.InterviewSession;

import org.springframework.stereotype.Service;
import za.ac.skiet.vyou.domain.InterviewSession;
import za.ac.skiet.vyou.service.IService;

import java.util.List;
import java.util.UUID;

public interface IInterviewSessionService extends IService<InterviewSession, UUID> {
    List<InterviewSession> getAll();
    List<InterviewSession> findByUserId(UUID userId);
}

