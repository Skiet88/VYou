package za.ac.skiet.vyou.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.skiet.vyou.domain.AiEvaluation;

import java.util.UUID;

@Repository
public interface AiEvaluationRepository
        extends JpaRepository<AiEvaluation, UUID> {
}

