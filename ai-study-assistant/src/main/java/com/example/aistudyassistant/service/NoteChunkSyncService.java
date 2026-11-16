package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.ChunkPreview;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

@Service
public class NoteChunkSyncService {

    private final NoteChunkPreviewService previewService;
    private final NoteChunkVectorService vectorService;

    public NoteChunkSyncService(
            NoteChunkPreviewService previewService,
            NoteChunkVectorService vectorService
    ) {
        this.previewService = previewService;
        this.vectorService = vectorService;
    }

    // 将当前用户一篇笔记的已有片段写入 Qdrant
    public OptionalInt copyExistingChunks(Long userId, Long noteId) {
        Optional<List<ChunkPreview>> result =
                previewService.findPreviews(userId, noteId);

        // 笔记不存在或不属于当前用户
        if (result.isEmpty()) {
            return OptionalInt.empty();
        }

        List<ChunkPreview> chunks = result.get();

        for (ChunkPreview chunk : chunks) {
            vectorService.saveChunk(
                    userId,
                    noteId,
                    chunk.chunkIndex(),
                    chunk.content()
            );
        }

        return OptionalInt.of(chunks.size());
    }
}