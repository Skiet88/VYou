package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.schemas.RegisterUserRequest;
import za.ac.skiet.vyou2.schemas.SessionResponse;
import za.ac.skiet.vyou2.schemas.UserResponse;
import za.ac.skiet.vyou2.service.InterviewFacade;
import za.ac.skiet.vyou2.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final InterviewFacade interviewFacade;

    public UserController(UserService userService, InterviewFacade interviewFacade) {
        this.userService = userService;
        this.interviewFacade = interviewFacade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return SchemaMapper.toResponse(userService.register(request.email(), request.displayName()));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return SchemaMapper.toResponse(userService.get(id));
    }

    @GetMapping("/{id}/interviews")
    public List<SessionResponse> interviews(@PathVariable UUID id) {
        return interviewFacade.sessionsForUser(id).stream().map(SchemaMapper::toResponse).toList();
    }
}
