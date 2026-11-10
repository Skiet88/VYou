package za.ac.skiet.vyou2.repository;

import za.ac.skiet.vyou2.domain.InterviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewSummaryRepository extends JpaRepository<InterviewSummary, UUID> {

    Optional<InterviewSummary> findBySessionId(UUID sessionId);

    /**
     * Just the (unencrypted) average for each session on a page: the encrypted feedback text is
     * neither loaded nor decrypted for a list view.
     */
    @Query("select s.sessionId as sessionId, s.averageOverall as averageOverall from InterviewSummary s "
            + "where s.sessionId in :sessionIds")
    List<SessionAverage> averagesForSessions(@Param("sessionIds") Collection<UUID> sessionIds);

    interface SessionAverage {
        UUID getSessionId();

        double getAverageOverall();
    }
}
