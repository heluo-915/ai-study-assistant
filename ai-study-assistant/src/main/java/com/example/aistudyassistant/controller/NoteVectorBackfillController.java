package com.example.aistudyassistant.controller;
import com.example.aistudyassistant.service.NoteVectorSyncTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class NoteVectorBackfillController {
    private final NoteVectorSyncTaskService tasks;
    // 初始化 NoteVectorBackfillController 实例。
    public NoteVectorBackfillController(NoteVectorSyncTaskService tasks) { this.tasks=tasks; }
    // 不触碰其他用户，也不覆盖已经存在的任务状态。
    @PostMapping("/notes/vector-sync/backfill")
    public ResponseEntity<Void> backfill(@RequestAttribute("userId") Long userId) {
        tasks.scheduleMissing(userId);
        return ResponseEntity.accepted().build();
    }
}
