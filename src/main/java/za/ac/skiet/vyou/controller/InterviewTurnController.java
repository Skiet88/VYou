package za.ac.skiet.vyou.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.skiet.vyou.DTO.InterviewTurnDTO;
import za.ac.skiet.vyou.domain.InterviewTurn;
import za.ac.skiet.vyou.service.InterviewTurn.IInterviewTurnService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/interview-turn")
public class InterviewTurnController {

    private final IInterviewTurnService service;

    @Autowired
    public InterviewTurnController(IInterviewTurnService service) {
        this.service = service;
    }

    // ================= CREATE =================
    @PostMapping
    public ResponseEntity<InterviewTurnDTO> create(@RequestBody InterviewTurn turn) {
        InterviewTurn created = service.create(turn);
        if (created == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(toDTO(created));
    }

    // ================= READ =================
    @GetMapping("/{id}")
    public ResponseEntity<InterviewTurnDTO> read(@PathVariable UUID id) {
        InterviewTurn turn = service.read(id);
        if (turn == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(turn));
    }

    // ================= UPDATE =================
    @PutMapping("/{id}")
    public ResponseEntity<InterviewTurnDTO> update(
            @PathVariable UUID id,
            @RequestBody InterviewTurn turn) {
        turn.setId(id);
        InterviewTurn updated = service.update(turn);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    // ================= DELETE =================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return service.delete(id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    // ================= GET ALL =================
    @GetMapping
    public ResponseEntity<List<InterviewTurnDTO>> getAll() {
        List<InterviewTurnDTO> dtoList = service.getAll()
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(dtoList);
    }

    // ================= GET BY SESSION =================
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<InterviewTurnDTO>> getBySession(@PathVariable UUID sessionId) {
        List<InterviewTurn> turns = service.findByInterviewSessionId(sessionId);
        if (turns.isEmpty()) return ResponseEntity.notFound().build();

        List<InterviewTurnDTO> dtoList = turns.stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(dtoList);
    }

    // ================= DTO MAPPER =================
    private InterviewTurnDTO toDTO(InterviewTurn turn) {
        InterviewTurnDTO dto = new InterviewTurnDTO();

        dto.setId(turn.getId());
        dto.setSequenceNumber(turn.getSequenceNumber());

        // Session
        if (turn.getInterviewSession() != null) {
            dto.setInterviewSessionId(turn.getInterviewSession().getId());
        }

        // Question
        if (turn.getQuestionContent() != null) {
            dto.setQuestionContentId(turn.getQuestionContent().getId());
            dto.setQuestionText(turn.getQuestionContent().getTextData());
        }

        // Answer
        if (turn.getAnswerContent() != null) {
            dto.setAnswerContentId(turn.getAnswerContent().getId());
            dto.setAnswerText(turn.getAnswerContent().getTextData());
        }

        // AI Evaluation
        if (turn.getAiEvaluation() != null) {
            dto.setAiEvaluationId(turn.getAiEvaluation().getId());
            dto.setAiOverallScore(turn.getAiEvaluation().getOverallScore());
            dto.setAiStrengths(turn.getAiEvaluation().getStrengths());
            dto.setAiImprovements(turn.getAiEvaluation().getImprovements());
        }

        return dto;
    }

//    @PostMapping("/{id}/answer")
//    public ResponseEntity<InterviewTurnDTO> submitAnswer(
//            @PathVariable UUID id,
//            @RequestBody Map<String, String> payload) {
//
//        String answer = payload.get("answer");
//        InterviewTurn updatedTurn = service.submitAnswer(id, answer);
//
//        return ResponseEntity.ok(toDTO(updatedTurn));
//    }

}
