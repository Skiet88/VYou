package za.ac.skiet.vyou.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.skiet.vyou.domain.InterviewTurn;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewTurnRepository
        extends JpaRepository<InterviewTurn, UUID> {

    List<InterviewTurn> findByInterviewSessionIdOrderBySequenceNumber(UUID interviewSessionId);
}

