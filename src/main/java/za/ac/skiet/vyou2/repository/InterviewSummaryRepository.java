package za.ac.skiet.vyou2.repository;

import za.ac.skiet.vyou2.domain.InterviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InterviewSummaryRepository extends JpaRepository<InterviewSummary, UUID> {

    Optional<InterviewSummary> findBySessionId(UUID sessionId);
}
