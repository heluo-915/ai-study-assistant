package com.example.aistudyassistant.controller;
import com.example.aistudyassistant.dto.ChunkBuildJob;
import com.example.aistudyassistant.service.ChunkBuildJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChunkBuildJobController {
    private final ChunkBuildJobService service;
    // 初始化 ChunkBuildJobController 实例。
    public ChunkBuildJobController(ChunkBuildJobService service) { this.service=service; }
    // 为当前用户的笔记创建片段构建任务。
    @PostMapping("/notes/{noteId}/chunk-jobs")
    public ResponseEntity<JobCreated> create(@RequestAttribute("userId") Long userId,@PathVariable Long noteId) {
        Long id=service.create(userId,noteId);
        return id==null ? ResponseEntity.notFound().build() : ResponseEntity.accepted().body(new JobCreated(id));
    }
    public record JobCreated(Long jobId) {}
    // 查询当前用户的片段构建任务。
    @GetMapping("/chunk-jobs/{id}")
    public ResponseEntity<ChunkBuildJob> find(@RequestAttribute("userId") Long userId,@PathVariable Long id) {
        ChunkBuildJob job=service.find(userId,id);
        return job==null ? ResponseEntity.notFound().build() : ResponseEntity.ok(job);
    }
    // 将当前用户可重试的构建任务重新排队。
    @PostMapping("/chunk-jobs/{id}/retry")
    public ResponseEntity<Void> retry(@RequestAttribute("userId") Long userId,@PathVariable Long id) {
        return service.retry(userId,id) ? ResponseEntity.accepted().build() : ResponseEntity.notFound().build();
    }
}
