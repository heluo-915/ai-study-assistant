package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.NoteVectorSyncTask;
import com.example.aistudyassistant.mapper.NoteVectorSyncTaskMapper;
import com.example.aistudyassistant.service.NoteVectorSyncTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notes/{noteId}/vector-sync")
public class NoteVectorSyncController {
    private final NoteVectorSyncTaskService tasks;
    private final NoteVectorSyncTaskMapper mapper;
    // 初始化 NoteVectorSyncController 实例。
    public NoteVectorSyncController(NoteVectorSyncTaskService tasks, NoteVectorSyncTaskMapper mapper) {
        this.tasks=tasks; this.mapper=mapper;
    }
    // 只能为本人的现有笔记安排同步，适用于已有笔记的回填。
    @PostMapping
    public ResponseEntity<Void> schedule(@RequestAttribute("userId") Long userId, @PathVariable Long noteId) {
        return tasks.schedule(userId,noteId,NoteVectorSyncTaskService.Action.UPSERT)
                ? ResponseEntity.accepted().build() : ResponseEntity.notFound().build();
    }
    // 查询当前用户指定笔记的向量同步状态。
    @GetMapping
    public ResponseEntity<NoteVectorSyncTask> status(@RequestAttribute("userId") Long userId, @PathVariable Long noteId) {
        return tasks.find(userId,noteId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    // 将当前用户失败的向量同步任务重新排队。
    @PostMapping("/retry")
    public ResponseEntity<Void> retry(@RequestAttribute("userId") Long userId, @PathVariable Long noteId) {
        return mapper.retry(userId,noteId)>0 ? ResponseEntity.accepted().build() : ResponseEntity.notFound().build();
    }
}
