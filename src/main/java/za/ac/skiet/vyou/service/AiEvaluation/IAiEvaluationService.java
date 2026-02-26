package za.ac.skiet.vyou.service.AiEvaluation;


import org.springframework.stereotype.Service;
import za.ac.skiet.vyou.domain.AiEvaluation;
import za.ac.skiet.vyou.service.IService;

import java.util.List;
import java.util.UUID;

public interface IAiEvaluationService extends IService<AiEvaluation, UUID> {
    List<AiEvaluation> getAll();
}

