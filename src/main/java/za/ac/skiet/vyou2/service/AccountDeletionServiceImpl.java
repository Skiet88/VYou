package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.domain.UserRole;
import za.ac.skiet.vyou2.domain.UserStatus;
import za.ac.skiet.vyou2.exception.ConflictException;
import za.ac.skiet.vyou2.exception.IncorrectPasswordException;
import za.ac.skiet.vyou2.exception.ResourceNotFoundException;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.repository.InterviewSessionRepository;
import za.ac.skiet.vyou2.repository.InterviewSummaryRepository;
import za.ac.skiet.vyou2.repository.InterviewTurnRepository;
import za.ac.skiet.vyou2.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccountDeletionServiceImpl implements AccountDeletionService {

    public static final int GRACE_PERIOD_DAYS = 30;
    private static final Duration GRACE_PERIOD = Duration.ofDays(GRACE_PERIOD_DAYS);
    private static final Logger audit = LoggerFactory.getLogger("vyou.audit");

    private final UserRepository users;
    private final InterviewSessionRepository sessions;
    private final InterviewTurnRepository turns;
    private final InterviewSummaryRepository summaries;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountDeletionServiceImpl(UserRepository users, InterviewSessionRepository sessions,
                                      InterviewTurnRepository turns, InterviewSummaryRepository summaries,
                                      PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.turns = turns;
        this.summaries = summaries;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void requestDeletion(UUID userId, String rawPassword) {
        UserAccount user = find(userId);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IncorrectPasswordException("Incorrect password");
        }
        if (user.getRole() == UserRole.ADMIN && users.countByRole(UserRole.ADMIN) <= 1) {
            throw new ConflictException("You're the only admin. Promote another admin before deleting your account.");
        }
        Instant purgeAt = clock.instant().plus(GRACE_PERIOD);
        users.save(UserFactory.scheduleDeletion(user, purgeAt));
        audit.info("user {} requested account deletion, scheduled for {}", userId, purgeAt);
    }

    @Override
    @Transactional
    public UserAccount cancelDeletion(UUID userId) {
        UserAccount user = find(userId);
        if (!user.isPendingDeletion()) {
            return user;
        }
        UserAccount restored = users.save(UserFactory.cancelDeletion(user));
        audit.info("user {} signed in and cancelled their scheduled account deletion", userId);
        return restored;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findDueForPurge(Instant now) {
        return users.findByStatusAndDeletionScheduledAtLessThanEqual(UserStatus.PENDING_DELETION, now)
                .stream().map(UserAccount::getId).toList();
    }

    @Override
    @Transactional
    public void purgeOne(UUID userId) {
        UserAccount user = users.findById(userId).orElse(null);
        // Already gone, or the user signed back in and cancelled the deletion since the job scanned.
        if (user == null || !user.isPendingDeletion()) {
            return;
        }
        List<UUID> sessionIds = sessions.findAllByUserId(userId).stream().map(InterviewSession::getId).toList();
        if (!sessionIds.isEmpty()) {
            turns.deleteBySessionIdIn(sessionIds);
            summaries.deleteBySessionIdIn(sessionIds);
        }
        sessions.deleteByUserId(userId);
        users.delete(user);
        audit.info("purged account {} after its {}-day deletion grace period", userId, GRACE_PERIOD_DAYS);
    }

    private UserAccount find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found"));
    }
}
