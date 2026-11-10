package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.UserAccount;
import za.ac.skiet.vyou2.domain.UserStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * What an admin can see and do. Deliberately no access to interview content (answers, feedback,
 * summaries): only account details and aggregate numbers.
 */
public interface UserAdministrationService {

    PageResult<UserOverview> listUsers(String search, UserStatus status, int page, int size);

    /** @throws za.ac.skiet.vyou2.exception.ConflictException for the admin's own account or another admin's */
    UserOverview suspend(UUID actingAdminId, UUID userId);

    UserOverview reinstate(UUID actingAdminId, UUID userId);

    record UserOverview(UserAccount user, int interviewsTotal, int interviewsCompleted, Optional<Double> averageOverall) {
    }
}
