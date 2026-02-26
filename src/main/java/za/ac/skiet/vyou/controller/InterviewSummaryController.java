package za.ac.skiet.vyou.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.skiet.vyou.DTO.InterviewSummaryDTO;
import za.ac.skiet.vyou.domain.InterviewSummary;
import za.ac.skiet.vyou.service.InterviewSummary.IInterviewSummaryService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/interview-summary")
public class InterviewSummaryController {

    private final IInterviewSummaryService service;

    @Autowired
    public InterviewSummaryController(IInterviewSummaryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InterviewSummary> create(@RequestBody InterviewSummary summary) {
        return ResponseEntity.ok(service.create(summary));
    }


    @GetMapping("/{id}")
    public ResponseEntity<InterviewSummaryDTO> read(@PathVariable UUID id) {
        InterviewSummary summary = service.read(id);
        if (summary == null) return ResponseEntity.notFound().build();

        // Map entity → DTO
        InterviewSummaryDTO dto = new InterviewSummaryDTO();
        dto.setId(summary.getId());
        dto.setOverallScore(summary.getOverallScore());
        dto.setGeneralFeedback(summary.getGeneralFeedback());
        dto.setRecommendations(summary.getRecommendations());
        dto.setCreatedAt(summary.getCreatedAt());

        if (summary.getInterviewSession() != null) {
            dto.setSessionId(summary.getInterviewSession().getId());
            dto.setSessionJobRole(summary.getInterviewSession().getJobRole());
            dto.setSessionLevel(summary.getInterviewSession().getLevel());
            dto.setSessionMode(summary.getInterviewSession().getMode());
            dto.setSessionStatus(summary.getInterviewSession().getStatus());
        }

        return ResponseEntity.ok(dto);
    }
    @PutMapping("/{id}")
    public ResponseEntity<InterviewSummaryDTO> update(@PathVariable UUID id, @RequestBody InterviewSummary summary) {
        summary.setId(id); // ensure entity has correct ID

        InterviewSummary updated = service.update(summary);

        if (updated == null) return ResponseEntity.notFound().build();

        // Map entity → DTO
        InterviewSummaryDTO dto = new InterviewSummaryDTO();
        dto.setId(updated.getId());
        dto.setOverallScore(updated.getOverallScore());
        dto.setGeneralFeedback(updated.getGeneralFeedback());
        dto.setRecommendations(updated.getRecommendations());
        dto.setCreatedAt(updated.getCreatedAt());

        if (updated.getInterviewSession() != null) {
            dto.setSessionId(updated.getInterviewSession().getId());
            dto.setSessionJobRole(updated.getInterviewSession().getJobRole());
            dto.setSessionLevel(updated.getInterviewSession().getLevel());
            dto.setSessionMode(updated.getInterviewSession().getMode());
            dto.setSessionStatus(updated.getInterviewSession().getStatus());
        }

        return ResponseEntity.ok(dto);
    }


//    @GetMapping("/{id}")
//    public ResponseEntity<InterviewSummary> read(@PathVariable UUID id) {
//        InterviewSummary summary = service.read(id);
//        if (summary == null) return ResponseEntity.notFound().build();
//        return ResponseEntity.ok(summary);
//    }


//    @PutMapping("/{id}")
//    public ResponseEntity<InterviewSummary> update(@PathVariable UUID id, @RequestBody InterviewSummary summary) {
//        summary.setId(id);
//        return ResponseEntity.ok(service.update(summary));
//    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return service.delete(id) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<InterviewSummaryDTO>> getAll() {
        List<InterviewSummary> summaries = service.getAll();

        // Map each entity to DTO
        List<InterviewSummaryDTO> dtoList = summaries.stream().map(summary -> {
            InterviewSummaryDTO dto = new InterviewSummaryDTO();
            dto.setId(summary.getId());
            dto.setOverallScore(summary.getOverallScore());
            dto.setGeneralFeedback(summary.getGeneralFeedback());
            dto.setRecommendations(summary.getRecommendations());
            dto.setCreatedAt(summary.getCreatedAt());

            if (summary.getInterviewSession() != null) {
                dto.setSessionId(summary.getInterviewSession().getId());
                dto.setSessionJobRole(summary.getInterviewSession().getJobRole());
                dto.setSessionLevel(summary.getInterviewSession().getLevel());
                dto.setSessionMode(summary.getInterviewSession().getMode());
                dto.setSessionStatus(summary.getInterviewSession().getStatus());
            }

            return dto;
        }).toList();

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<InterviewSummary> getBySession(@PathVariable UUID sessionId) {
        InterviewSummary summary = service.findByInterviewSessionId(sessionId);
        if (summary == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(summary);
    }
}
