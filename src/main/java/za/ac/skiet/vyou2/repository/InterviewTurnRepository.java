package za.ac.skiet.vyou2.repository;

import za.ac.skiet.vyou2.domain.InterviewTurn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewTurnRepository extends JpaRepository<InterviewTurn, UUID> {

    List<InterviewTurn> findBySessionIdOrderBySequenceNumberAsc(UUID sessionId);

    int countBySessionId(UUID sessionId);
}
