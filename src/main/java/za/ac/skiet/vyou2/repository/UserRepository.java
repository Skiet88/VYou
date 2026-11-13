package za.ac.skiet.vyou2.repository;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.domain.UserRole;
import za.ac.skiet.vyou2.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {

    boolean existsByEmail(String email);

    Optional<UserAccount> findByEmail(String email);

    /**
     * Admin search. {@code pattern} is a lower-cased LIKE pattern built by the service with its
     * own wildcards escaped; it is always a bound parameter, never concatenated into the query.
     */
    @Query("select u from UserAccount u where (:status is null or u.status = :status) and "
            + "(:pattern is null or lower(u.email) like :pattern escape '\\' or lower(u.displayName) like :pattern escape '\\')")
    Page<UserAccount> search(@Param("pattern") String pattern, @Param("status") UserStatus status, Pageable pageable);

    long countByRole(UserRole role);

    /** Accounts whose grace period has elapsed; the purge job deletes each one. */
    List<UserAccount> findByStatusAndDeletionScheduledAtLessThanEqual(UserStatus status, Instant cutoff);

    /** Looked up by the hash of the raw token from a verification link; the raw token is never stored. */
    Optional<UserAccount> findByVerificationTokenHash(String tokenHash);
}
