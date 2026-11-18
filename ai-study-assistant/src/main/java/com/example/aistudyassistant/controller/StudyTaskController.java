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
    // 初始化 StudyTaskController 实例。
    public StudyTaskController(StudyTaskService service) { this.service=service; }
    // 为当前用户生成当天的学习任务。
    @PostMapping("/generate")
    public List<StudyTask> generate(@RequestAttribute("userId") Long userId) {
        return service.generate(userId, LocalDate.now());
    }
    // 查询当前用户指定日期的学习任务。
    @GetMapping
    public List<StudyTask> find(@RequestAttribute("userId") Long userId,
            @RequestParam(required=false) LocalDate date) {
        return service.find(userId, date == null ? LocalDate.now() : date);
    }
    // 更新当前用户学习任务的完成状态。
    @PutMapping("/{taskId}")
    public ResponseEntity<Void> update(@RequestAttribute("userId") Long userId,
            @PathVariable Long taskId, @RequestBody UpdateStudyTaskRequest request) {
        if (request.completed() == null) return ResponseEntity.badRequest().build();
        return service.setCompleted(userId, taskId, request.completed())
                ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    public record UpdateStudyTaskRequest(Boolean completed) {}
}
