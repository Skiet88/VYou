package za.ac.skiet.vyou2.repository;

import za.ac.skiet.vyou2.domain.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    List<InterviewSession> findByUserIdOrderByStartedAtDesc(UUID userId);
}
