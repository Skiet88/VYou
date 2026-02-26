package za.ac.skiet.vyou.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.skiet.vyou.domain.InterviewSession;
import za.ac.skiet.vyou.service.InterviewSession.IInterviewSessionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/interview-session")
public class InterviewSessionController {

    private final IInterviewSessionService service;

    @Autowired
    public InterviewSessionController(IInterviewSessionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InterviewSession> create(@RequestBody InterviewSession session) {
        return ResponseEntity.ok(service.create(session));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewSession> read(@PathVariable UUID id) {
        InterviewSession session = service.read(id);
        if (session == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(session);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InterviewSession> update(@PathVariable UUID id, @RequestBody InterviewSession session) {
        session.setId(id);
        return ResponseEntity.ok(service.update(session));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return service.delete(id) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<InterviewSession>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InterviewSession>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.findByUserId(userId));
    }
}
