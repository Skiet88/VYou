package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.schemas.LoginRequest;
import za.ac.skiet.vyou2.schemas.SignupRequest;
import za.ac.skiet.vyou2.schemas.UserResponse;
import za.ac.skiet.vyou2.security.AppUserDetails;
import za.ac.skiet.vyou2.security.CurrentUser;
import za.ac.skiet.vyou2.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserService userService, AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        userService.register(request.email(), request.displayName(), request.password());
        // Log the new user in immediately rather than making them submit the same credentials twice.
        authenticate(request.email(), request.password(), httpRequest, httpResponse);
        return SchemaMapper.toResponse(currentUser());
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {
        authenticate(request.email(), request.password(), httpRequest, httpResponse);
        return SchemaMapper.toResponse(currentUser());
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return SchemaMapper.toResponse(userService.get(CurrentUser.id(authentication)));
    }

    /** Same generic failure for "no such email" and "wrong password", so login can't be used to probe emails. */
    private void authenticate(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication authRequest = UsernamePasswordAuthenticationToken.unauthenticated(email.strip().toLowerCase(), password);
        Authentication result;
        try {
            result = authenticationManager.authenticate(authRequest);
        } catch (org.springframework.security.authentication.DisabledException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private za.ac.skiet.vyou2.domain.UserAccount currentUser() {
        AppUserDetails details = (AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return details.getUser();
    }
}
