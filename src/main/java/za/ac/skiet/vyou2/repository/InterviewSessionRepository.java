package za.ac.skiet.vyou2.repository;

import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    Page<InterviewSession> findByUserId(UUID userId, Pageable pageable);

    Page<InterviewSession> findByUserIdAndStatus(UUID userId, InterviewStatus status, Pageable pageable);

    /** Unpaged: used only by account purge, which needs every session id for a user at once. */
    List<InterviewSession> findAllByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    /** Interview totals per user for a page of users, in one grouped query. */
    @Query("select s.userId as userId, count(s) as total, "
            + "sum(case when s.status = za.ac.skiet.vyou2.domain.InterviewStatus.COMPLETED then 1 else 0 end) as completed "
            + "from InterviewSession s where s.userId in :userIds group by s.userId")
    List<UserInterviewCounts> countsForUsers(@Param("userIds") Collection<UUID> userIds);

    interface UserInterviewCounts {
        UUID getUserId();

        long getTotal();

        long getCompleted();
    }
}
