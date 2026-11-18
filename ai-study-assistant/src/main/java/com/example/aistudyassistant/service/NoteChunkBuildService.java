package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.ChunkEmbedding;
import com.example.aistudyassistant.entity.Note;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteChunkBuildService {

    private final NoteService noteService;
    private final NoteChunkService noteChunkService;
    private final NoteChunkPersistenceService noteChunkPersistenceService;

    // 初始化 NoteChunkBuildService 实例。
    public NoteChunkBuildService(
            NoteService noteService,
            NoteChunkService noteChunkService,
            NoteChunkPersistenceService noteChunkPersistenceService
    ) {
        this.noteService = noteService;
        this.noteChunkService = noteChunkService;
        this.noteChunkPersistenceService = noteChunkPersistenceService;
    }

    // 为当前用户的笔记生成并保存新片段。
    public boolean buildChunks(Long userId, Long noteId) {
        Note note = noteService.findByIdAndUserId(userId, noteId);

        if (note == null) {
            return false;
        }

        String originalContent = note.getContent();

        // 在事务外调用 Ollama，准备所有片段。
        List<ChunkEmbedding> chunks =
                noteChunkService.generateChunks(originalContent);

        // 在短事务内重新检查归属和正文，再保存。
        noteChunkPersistenceService.insertChunksIfUnchanged(
                userId, noteId, originalContent, chunks
        );

        return true;
    }
}