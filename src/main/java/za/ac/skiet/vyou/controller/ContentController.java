package za.ac.skiet.vyou.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.skiet.vyou.domain.Content;
import za.ac.skiet.vyou.service.Content.IContentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/content")
public class ContentController {

    private final IContentService service;

    @Autowired
    public ContentController(IContentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Content> create(@RequestBody Content content) {
        return ResponseEntity.ok(service.create(content));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Content> read(@PathVariable UUID id) {
        Content content = service.read(id);
        if (content == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(content);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Content> update(@PathVariable UUID id, @RequestBody Content content) {
        content.setId(id);
        return ResponseEntity.ok(service.update(content));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return service.delete(id) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Content>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
}
