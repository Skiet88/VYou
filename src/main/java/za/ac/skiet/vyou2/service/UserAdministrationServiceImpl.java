package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.domain.UserRole;
import za.ac.skiet.vyou2.domain.UserStatus;
import za.ac.skiet.vyou2.exception.ConflictException;
import za.ac.skiet.vyou2.exception.DomainValidationException;
import za.ac.skiet.vyou2.exception.ResourceNotFoundException;
import za.ac.skiet.vyou2.factory.UserFactory;
import za.ac.skiet.vyou2.repository.InterviewSessionRepository;
import za.ac.skiet.vyou2.repository.InterviewSummaryRepository;
import za.ac.skiet.vyou2.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserAdministrationServiceImpl implements UserAdministrationService {

    public static final int MAX_SEARCH_LENGTH = 100;
    private static final Logger audit = LoggerFactory.getLogger("vyou.audit");

    private final UserRepository users;
    private final InterviewSessionRepository sessions;
    private final InterviewSummaryRepository summaries;

    public UserAdministrationServiceImpl(UserRepository users, InterviewSessionRepository sessions,
                                         InterviewSummaryRepository summaries) {
        this.users = users;
        this.sessions = sessions;
        this.summaries = summaries;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserOverview> listUsers(String search, UserStatus status, int page, int size) {
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<UserAccount> result = users.search(likePattern(search), status, request);
        List<UUID> ids = result.getContent().stream().map(UserAccount::getId).toList();
        Map<UUID, InterviewSessionRepository.UserInterviewCounts> counts = ids.isEmpty() ? Map.of()
                : sessions.countsForUsers(ids).stream().collect(Collectors.toMap(InterviewSessionRepository.UserInterviewCounts::getUserId, c -> c));
        Map<UUID, Double> averages = ids.isEmpty() ? Map.of()
                : summaries.averagesForUsers(ids).stream().collect(Collectors.toMap(
                        InterviewSummaryRepository.UserAverage::getUserId, a -> Math.round(a.getAverageOverall() * 10) / 10.0));
        List<UserOverview> items = result.getContent().stream().map(user -> {
            var c = counts.get(user.getId());
            return new UserOverview(user, c == null ? 0 : (int) c.getTotal(), c == null ? 0 : (int) c.getCompleted(),
                    Optional.ofNullable(averages.get(user.getId())));
        }).toList();
        return new PageResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    @Transactional
    public UserOverview suspend(UUID actingAdminId, UUID userId) {
        UserAccount target = find(userId);
        if (target.getId().equals(actingAdminId)) {
            throw new ConflictException("You can't suspend your own account");
        }
        if (target.getRole() == UserRole.ADMIN) {
            throw new ConflictException("Admin accounts can't be suspended here");
        }
        UserAccount updated = target.getStatus() == UserStatus.SUSPENDED ? target
                : users.save(UserFactory.withStatus(target, UserStatus.SUSPENDED));
        // Ids only: no names or emails in logs.
        audit.info("admin {} suspended user {}", actingAdminId, userId);
        return overview(updated);
    }

    @Override
    @Transactional
    public UserOverview reinstate(UUID actingAdminId, UUID userId) {
        UserAccount target = find(userId);
        UserAccount updated = target.getStatus() == UserStatus.ACTIVE ? target
                : users.save(UserFactory.withStatus(target, UserStatus.ACTIVE));
        audit.info("admin {} reinstated user {}", actingAdminId, userId);
        return overview(updated);
    }

    private UserAccount find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found"));
    }

    private UserOverview overview(UserAccount user) {
        List<UUID> ids = List.of(user.getId());
        var counts = sessions.countsForUsers(ids).stream().findFirst();
        var average = summaries.averagesForUsers(ids).stream().findFirst()
                .map(a -> Math.round(a.getAverageOverall() * 10) / 10.0);
        return new UserOverview(user, counts.map(c -> (int) c.getTotal()).orElse(0),
                counts.map(c -> (int) c.getCompleted()).orElse(0), average);
    }

    /** Lower-cased "contains" pattern with LIKE wildcards in the user's text escaped. */
    static String likePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String trimmed = search.strip();
        if (trimmed.length() > MAX_SEARCH_LENGTH) {
            throw new DomainValidationException("Search must be " + MAX_SEARCH_LENGTH + " characters or fewer");
        }
        String escaped = trimmed.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
