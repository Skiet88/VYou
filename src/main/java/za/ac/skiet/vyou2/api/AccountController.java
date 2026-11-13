package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.schemas.DeleteAccountRequest;
import za.ac.skiet.vyou2.security.CurrentUser;
import za.ac.skiet.vyou2.service.AccountDeletionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountDeletionService deletions;

    public AccountController(AccountDeletionService deletions) {
        this.deletions = deletions;
    }

    /**
     * Schedules the caller's account for deletion 30 days out and signs them out everywhere, like
     * logout. Signing back in before then cancels it (AuthController).
     */
    @PostMapping("/delete")
    public void delete(@Valid @RequestBody DeleteAccountRequest request, Authentication authentication,
                       HttpServletRequest httpRequest) {
        deletions.requestDeletion(CurrentUser.id(authentication), request.password());
        SecurityContextHolder.clearContext();
        var session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
