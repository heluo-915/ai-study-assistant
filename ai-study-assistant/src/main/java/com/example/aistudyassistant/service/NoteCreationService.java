package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.CreateNoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NoteCreationService {

    private static final Logger log =
            LoggerFactory.getLogger(NoteCreationService.class);

    private final NoteService noteService;
    private final NoteChunkBuildService noteChunkBuildService;

    public NoteCreationService(
            NoteService noteService,
            NoteChunkBuildService noteChunkBuildService
    ) {
        this.noteService = noteService;
        this.noteChunkBuildService = noteChunkBuildService;
    }

    // 创建笔记后尝试生成片段，并返回两阶段的结果。
    public CreateNoteResponse create(Long userId, String title, String content) {
        Long noteId = noteService.create(title, content, userId);

        try {
            boolean success = noteChunkBuildService.buildChunks(userId, noteId);

            if (!success) {
                return new CreateNoteResponse(
                        noteId, false, "笔记已创建，但未能生成片段；笔记可能已被删除"
                );
            }

            return new CreateNoteResponse(
                    noteId, true, "笔记和片段创建成功"
            );
        } catch (RuntimeException exception) {
            log.warn("创建笔记后生成片段失败，笔记 ID：{}", noteId, exception);
            return new CreateNoteResponse(
                    noteId, false, "笔记已创建，片段生成失败，可稍后重试"
            );
        }
    }
}