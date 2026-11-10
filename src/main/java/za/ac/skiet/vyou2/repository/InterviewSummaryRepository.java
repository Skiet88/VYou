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

    /** Each user's mean summary score across their interviews (numbers only, no text), one query. */
    @Query("select sess.userId as userId, avg(s.averageOverall) as averageOverall from InterviewSummary s, InterviewSession sess "
            + "where s.sessionId = sess.id and sess.userId in :userIds group by sess.userId")
    List<UserAverage> averagesForUsers(@Param("userIds") Collection<UUID> userIds);

    interface UserAverage {
        UUID getUserId();

        double getAverageOverall();
    }

    interface SessionAverage {
        UUID getSessionId();

        double getAverageOverall();
    }
}
