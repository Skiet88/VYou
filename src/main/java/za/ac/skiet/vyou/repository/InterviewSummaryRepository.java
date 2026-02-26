package za.ac.skiet.vyou.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.skiet.vyou.domain.InterviewSummary;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewSummaryRepository
        extends JpaRepository<InterviewSummary, UUID> {

    Optional<InterviewSummary> findByInterviewSessionId(UUID interviewSessionId);
}
