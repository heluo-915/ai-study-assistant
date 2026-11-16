package com.example.aistudyassistant.controller;
import com.example.aistudyassistant.dto.StudyTask;
import com.example.aistudyassistant.service.StudyTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/study/tasks")
public class StudyTaskController {
    private final StudyTaskService service;
    public StudyTaskController(StudyTaskService service) { this.service=service; }
    @PostMapping("/generate")
    public List<StudyTask> generate(@RequestAttribute("userId") Long userId) {
        return service.generate(userId, LocalDate.now());
    }
    @GetMapping
    public List<StudyTask> find(@RequestAttribute("userId") Long userId,
            @RequestParam(required=false) LocalDate date) {
        return service.find(userId, date == null ? LocalDate.now() : date);
    }
    @PutMapping("/{taskId}")
    public ResponseEntity<Void> update(@RequestAttribute("userId") Long userId,
            @PathVariable Long taskId, @RequestBody UpdateStudyTaskRequest request) {
        if (request.completed() == null) return ResponseEntity.badRequest().build();
        return service.setCompleted(userId, taskId, request.completed())
                ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    public record UpdateStudyTaskRequest(Boolean completed) {}
}
