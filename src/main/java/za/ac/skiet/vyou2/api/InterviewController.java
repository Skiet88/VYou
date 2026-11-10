package za.ac.skiet.vyou2.api;

import za.ac.skiet.vyou2.schemas.AnswerResponse;
import za.ac.skiet.vyou2.schemas.SessionDetailsResponse;
import za.ac.skiet.vyou2.schemas.SessionResponse;
import za.ac.skiet.vyou2.schemas.StartInterviewRequest;
import za.ac.skiet.vyou2.schemas.SubmitAnswerRequest;
import za.ac.skiet.vyou2.service.InterviewFacade;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewFacade facade;

    public InterviewController(InterviewFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse start(@Valid @RequestBody StartInterviewRequest request) {
        return SchemaMapper.toResponse(facade.start(request.jobRole(), request.level(), request.modeOrDefault()));
    }

    @PostMapping("/{id}/answers")
    public AnswerResponse answer(@PathVariable UUID id, @Valid @RequestBody SubmitAnswerRequest request) {
        return SchemaMapper.toResponse(facade.submitAnswer(id, request.answer()));
    }

    @PostMapping("/{id}/complete")
    public SessionResponse complete(@PathVariable UUID id) {
        return SchemaMapper.toResponse(facade.complete(id));
    }

    @GetMapping("/{id}")
    public SessionDetailsResponse get(@PathVariable UUID id) {
        return SchemaMapper.toResponse(facade.details(id));
    }
}
