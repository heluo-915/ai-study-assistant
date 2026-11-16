package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.ChunkPreview;
import com.example.aistudyassistant.dto.NoteVectorSyncTask;
import com.example.aistudyassistant.mapper.NoteVectorSyncTaskMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** 手动消费一条同步任务；当前不自动调度，也不暴露 HTTP 接口。 */
@Service
public class NoteVectorSyncWorker {
    private final NoteVectorSyncTaskMapper tasks;
    private final NoteChunkPreviewService previews;
    private final NoteChunkVectorService vectors;

    public NoteVectorSyncWorker(NoteVectorSyncTaskMapper tasks,
                                NoteChunkPreviewService previews,
                                NoteChunkVectorService vectors) {
        this.tasks = tasks;
        this.previews = previews;
        this.vectors = vectors;
    }

    // 返回 false 表示没有领取到 PENDING 任务；写入失败时记录失败并向调用者抛出异常。
    public boolean processPending(Long userId, Long noteId) {
        if (userId == null || noteId == null) throw new IllegalArgumentException("参数不能为空");
        String token = UUID.randomUUID().toString();
        if (tasks.claim(userId, noteId, token) == 0) return false;

        try {
            NoteVectorSyncTask task = tasks.find(userId, noteId);
            if (task == null || task.claimedVersion() == null || task.claimedAction() == null) {
                throw new IllegalStateException("无法读取已领取的同步任务");
            }
            switch (task.claimedAction()) {
                case "UPSERT" -> replaceChunks(userId, noteId);
                case "DELETE" -> vectors.deleteByNote(userId, noteId);
                default -> throw new IllegalStateException("不支持的同步任务类型");
            }
        } catch (RuntimeException failure) {
            try {
                if (tasks.finish(userId, noteId, token, false) != 1) {
                    failure.addSuppressed(new IllegalStateException("未能更新同步任务状态"));
                }
            } catch (RuntimeException finishFailure) {
                failure.addSuppressed(finishFailure);
            }
            throw failure;
        }

        if (tasks.finish(userId, noteId, token, true) != 1) {
            throw new IllegalStateException("同步完成，但任务状态更新失败");
        }
        return true;
    }

    // 先检查归属和读取片段，再替换向量；跨库操作无法原子提交，失败需重试。
    private void replaceChunks(Long userId, Long noteId) {
        List<ChunkPreview> chunks = previews.findPreviews(userId, noteId)
                .orElseThrow(() -> new IllegalStateException("笔记不存在或不属于当前用户"));
        vectors.deleteByNote(userId, noteId);
        for (ChunkPreview chunk : chunks) {
            vectors.saveChunk(userId, noteId, chunk.chunkIndex(), chunk.content());
        }
    }
}
