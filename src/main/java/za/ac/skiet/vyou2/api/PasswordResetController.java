package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.schemas.PasswordResetConfirmation;
import za.ac.skiet.vyou2.schemas.PasswordResetRequest;
import za.ac.skiet.vyou2.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Both endpoints are public: a visitor who forgot their password is, by definition, signed out. */
@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {

    private final PasswordResetService resetService;
    private final FrontendLinks frontendLinks;

    public PasswordResetController(PasswordResetService resetService, FrontendLinks frontendLinks) {
        this.resetService = resetService;
        this.frontendLinks = frontendLinks;
    }

    /** Always 200, whether or not the email is registered - see PasswordResetService.requestReset. */
    @PostMapping("/request")
    public void request(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {
        resetService.requestReset(request.email(), frontendLinks.origin(httpRequest));
    }

    @PostMapping("/reset")
    public void reset(@Valid @RequestBody PasswordResetConfirmation request) {
        resetService.resetPassword(request.token(), request.newPassword());
    }
}
