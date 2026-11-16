package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.NoteVectorSyncTaskKey;
import com.example.aistudyassistant.mapper.NoteVectorSyncTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 默认关闭；仅在明确启用时轮询待处理任务。
@Component
@ConditionalOnProperty(prefix = "app.qdrant-sync", name = "enabled", havingValue = "true")
public class NoteVectorSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(NoteVectorSyncScheduler.class);
    private final NoteVectorSyncTaskMapper tasks;
    private final NoteVectorSyncWorker worker;
    public NoteVectorSyncScheduler(NoteVectorSyncTaskMapper tasks, NoteVectorSyncWorker worker) {
        this.tasks=tasks; this.worker=worker;
    }
    @Scheduled(fixedDelayString = "${app.qdrant-sync.poll-ms:5000}")
    public void processNext() {
        NoteVectorSyncTaskKey task = tasks.nextPending();
        if (task == null) return;
        try {
            worker.processPending(task.userId(), task.noteId());
        } catch (RuntimeException error) {
            log.warn("笔记向量同步任务失败，笔记 ID：{}，异常类型：{}", task.noteId(), error.getClass().getSimpleName());
        }
    }
}
