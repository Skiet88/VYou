package za.ac.skiet.vyou2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Daily sweep that permanently deletes accounts whose 30-day grace period has elapsed. Each
 * account is purged in its own call (through the injected service, so each gets its own
 * transaction); one failure is logged and does not stop the rest of the batch.
 */
@Component
public class AccountPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccountPurgeScheduler.class);

    private final AccountDeletionService deletions;
    private final Clock clock;

    public AccountPurgeScheduler(AccountDeletionService deletions, Clock clock) {
        this.deletions = deletions;
        this.clock = clock;
    }

    @Scheduled(cron = "${vyou.account.purge-cron:0 0 3 * * *}")
    public void purgeDueAccounts() {
        for (UUID userId : deletions.findDueForPurge(clock.instant())) {
            try {
                deletions.purgeOne(userId);
            } catch (RuntimeException ex) {
                log.error("Failed to purge account {}; will retry on the next run", userId, ex);
            }
        }
    }
}
