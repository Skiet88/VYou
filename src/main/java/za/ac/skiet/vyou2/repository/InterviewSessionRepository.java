package za.ac.skiet.vyou2.repository;

import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    Page<InterviewSession> findByUserId(UUID userId, Pageable pageable);

    Page<InterviewSession> findByUserIdAndStatus(UUID userId, InterviewStatus status, Pageable pageable);
}
