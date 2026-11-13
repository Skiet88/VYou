package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.schemas.VerifyEmailRequest;
import za.ac.skiet.vyou2.security.CurrentUser;
import za.ac.skiet.vyou2.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email-verification")
public class EmailVerificationController {

    private final EmailVerificationService verification;
    private final FrontendLinks frontendLinks;

    public EmailVerificationController(EmailVerificationService verification, FrontendLinks frontendLinks) {
        this.verification = verification;
        this.frontendLinks = frontendLinks;
    }

    /** Public: reachable from an email link on any device, signed in or not. */
    @PostMapping("/verify")
    public void verify(@Valid @RequestBody VerifyEmailRequest request) {
        verification.verify(request.token());
    }

    /** The caller's own account only; a no-op if already verified. */
    @PostMapping("/resend")
    public void resend(Authentication authentication, HttpServletRequest httpRequest) {
        verification.sendVerificationEmail(CurrentUser.id(authentication), frontendLinks.origin(httpRequest));
    }
}
