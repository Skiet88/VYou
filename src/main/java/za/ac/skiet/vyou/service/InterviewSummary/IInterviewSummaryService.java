package za.ac.skiet.vyou.service.InterviewSummary;

import org.springframework.stereotype.Service;
import za.ac.skiet.vyou.domain.InterviewSummary;
import za.ac.skiet.vyou.service.IService;

import java.util.List;
import java.util.UUID;

public interface IInterviewSummaryService extends IService<InterviewSummary, UUID> {
    List<InterviewSummary> getAll();
    InterviewSummary findByInterviewSessionId(UUID sessionId);
}
