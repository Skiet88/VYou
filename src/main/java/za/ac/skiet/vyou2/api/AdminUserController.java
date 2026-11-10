package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.domain.UserStatus;
import za.ac.skiet.vyou2.schemas.AdminUserResponse;
import za.ac.skiet.vyou2.schemas.PageResponse;
import za.ac.skiet.vyou2.security.CurrentUser;
import za.ac.skiet.vyou2.service.PageResult;
import za.ac.skiet.vyou2.service.UserAdministrationService;
import za.ac.skiet.vyou2.service.UserAdministrationService.UserOverview;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-only (enforced in SecurityConfig: /api/admin/** requires ROLE_ADMIN, with the role re-read
 * from the database on every request). Exposes no interview content.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAdministrationService administration;

    public AdminUserController(UserAdministrationService administration) {
        this.administration = administration;
    }

    @GetMapping
    public PageResponse<AdminUserResponse> list(
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        PageResult<AdminUserResponse> result = administration.listUsers(search, status, page, size).map(AdminUserController::toResponse);
        return new PageResponse<>(result.items(), result.page(), result.size(), result.totalItems(), result.totalPages());
    }

    @PostMapping("/{id}/suspend")
    public AdminUserResponse suspend(@PathVariable UUID id, Authentication authentication) {
        return toResponse(administration.suspend(CurrentUser.id(authentication), id));
    }

    @PostMapping("/{id}/reinstate")
    public AdminUserResponse reinstate(@PathVariable UUID id, Authentication authentication) {
        return toResponse(administration.reinstate(CurrentUser.id(authentication), id));
    }

    private static AdminUserResponse toResponse(UserOverview o) {
        var u = o.user();
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getStatus(), u.getCreatedAt(),
                o.interviewsTotal(), o.interviewsCompleted(), o.averageOverall().orElse(null));
    }
}
