package za.ac.skiet.vyou.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.skiet.vyou.ENUM.InterviewStatus;
import za.ac.skiet.vyou.domain.InterviewSession;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewSessionRepository
        extends JpaRepository<InterviewSession, UUID> {

    List<InterviewSession> findByUserId(UUID userId);

    List<InterviewSession> findByStatus(InterviewStatus status);
}

