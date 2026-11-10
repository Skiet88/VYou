package za.ac.skiet.vyou2.repository;

import za.ac.skiet.vyou2.domain.InterviewTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InterviewTurnRepository extends JpaRepository<InterviewTurn, UUID> {

    List<InterviewTurn> findBySessionIdOrderBySequenceNumberAsc(UUID sessionId);

    int countBySessionId(UUID sessionId);

    /** One grouped query for a whole page of sessions, instead of one count per session (N+1). */
    @Query("select t.sessionId as sessionId, count(t) as answered from InterviewTurn t "
            + "where t.sessionId in :sessionIds group by t.sessionId")
    List<SessionTurnCount> countBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);

    interface SessionTurnCount {
        UUID getSessionId();

        long getAnswered();
    }
}
