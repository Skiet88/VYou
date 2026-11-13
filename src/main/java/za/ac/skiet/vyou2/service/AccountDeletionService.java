package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.UserAccount;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Self-service account deletion with a grace period: requesting deletion schedules a purge
 * {@value AccountDeletionServiceImpl#GRACE_PERIOD_DAYS} days out rather than deleting immediately;
 * signing in again before then cancels it.
 */
public interface AccountDeletionService {

    /**
     * @throws za.ac.skiet.vyou2.exception.IncorrectPasswordException if {@code rawPassword} doesn't match
     * @throws za.ac.skiet.vyou2.exception.ConflictException if this is the only admin account
     */
    void requestDeletion(UUID userId, String rawPassword);

    /** No-op (not an error) if the account isn't pending deletion. */
    UserAccount cancelDeletion(UUID userId);

    /** Ids of accounts whose grace period has elapsed, for the purge job. */
    List<UUID> findDueForPurge(Instant now);

    /** Permanently deletes one account and everything it owns. No-op if it's no longer pending deletion. */
    void purgeOne(UUID userId);
}
