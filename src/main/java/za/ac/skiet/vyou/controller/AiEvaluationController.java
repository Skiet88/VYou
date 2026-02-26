package za.ac.skiet.vyou.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.skiet.vyou.domain.AiEvaluation;
import za.ac.skiet.vyou.service.AiEvaluation.IAiEvaluationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai-evaluation")
public class AiEvaluationController {

    private final IAiEvaluationService service;

    @Autowired
    public AiEvaluationController(IAiEvaluationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AiEvaluation> create(@RequestBody AiEvaluation eval) {
        return ResponseEntity.ok(service.create(eval));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AiEvaluation> read(@PathVariable UUID id) {
        AiEvaluation eval = service.read(id);
        if (eval == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(eval);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AiEvaluation> update(@PathVariable UUID id, @RequestBody AiEvaluation eval) {
        eval.setId(id);
        return ResponseEntity.ok(service.update(eval));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return service.delete(id) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<AiEvaluation>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
}
