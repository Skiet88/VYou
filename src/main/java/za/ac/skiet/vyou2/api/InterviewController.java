package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.schemas.AnswerResponse;
import za.ac.skiet.vyou2.schemas.CompletionResponse;
import za.ac.skiet.vyou2.schemas.SessionDetailsResponse;
import za.ac.skiet.vyou2.schemas.SessionResponse;
import za.ac.skiet.vyou2.schemas.StartInterviewRequest;
import za.ac.skiet.vyou2.schemas.SubmitAnswerRequest;
import za.ac.skiet.vyou2.schemas.SummaryResponse;
import za.ac.skiet.vyou2.security.CurrentUser;
import za.ac.skiet.vyou2.service.InterviewFacade;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Every endpoint acts for the authenticated caller only; no endpoint accepts a userId in the
 * request. Ownership of a specific interview is enforced inside InterviewFacade.
 */
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewFacade facade;

    public InterviewController(InterviewFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse start(@Valid @RequestBody StartInterviewRequest request, Authentication authentication) {
        UUID userId = CurrentUser.id(authentication);
        var session = facade.start(userId, request.jobRole(), request.level(), request.modeOrDefault(),
                request.feedbackModeOrDefault());
        return SchemaMapper.toResponse(session, 0);
    }

    @GetMapping("/mine")
    public List<SessionResponse> mine(Authentication authentication) {
        return facade.sessionsForUser(CurrentUser.id(authentication)).stream().map(SchemaMapper::toResponse).toList();
    }

    @PostMapping("/{id}/answers")
    public AnswerResponse answer(@PathVariable UUID id, @Valid @RequestBody SubmitAnswerRequest request,
                                 Authentication authentication) {
        return SchemaMapper.toResponse(facade.submitAnswer(CurrentUser.id(authentication), id, request.answer()));
    }

    @PostMapping("/{id}/complete")
    public CompletionResponse complete(@PathVariable UUID id, Authentication authentication) {
        return SchemaMapper.toResponse(facade.complete(CurrentUser.id(authentication), id));
    }

    /** Returns the summary, generating it first if completion couldn't (e.g. the AI was rate limited). */
    @PostMapping("/{id}/summary")
    public SummaryResponse summary(@PathVariable UUID id, Authentication authentication) {
        return SchemaMapper.toResponse(facade.summarise(CurrentUser.id(authentication), id));
    }

    @GetMapping("/{id}")
    public SessionDetailsResponse get(@PathVariable UUID id, Authentication authentication) {
        return SchemaMapper.toResponse(facade.details(CurrentUser.id(authentication), id));
    }
}
