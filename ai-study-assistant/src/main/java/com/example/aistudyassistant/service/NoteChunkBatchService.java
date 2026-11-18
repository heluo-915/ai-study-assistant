package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.BatchEmbeddingResult;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.mapper.NoteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NoteChunkBatchService {

    private static final Logger log =
            LoggerFactory.getLogger(NoteChunkBatchService.class);

    private final NoteMapper noteMapper;
    private final NoteChunkBuildService noteChunkBuildService;

    // 初始化 NoteChunkBatchService 实例。
    public NoteChunkBatchService(
            NoteMapper noteMapper,
            NoteChunkBuildService noteChunkBuildService
    ) {
        this.noteMapper = noteMapper;
        this.noteChunkBuildService = noteChunkBuildService;
    }

    // 为当前用户没有片段的笔记逐篇生成片段。
    public BatchEmbeddingResult buildMissingChunks(Long userId) {
        List<Note> notes = noteMapper.findMissingChunksByUserId(userId);
        List<Long> failedNoteIds = new ArrayList<>();
        int succeeded = 0;

        for (Note note : notes) {
            try {
                boolean success =
                        noteChunkBuildService.buildChunks(userId, note.getId());

                if (success) {
                    succeeded++;
                } else {
                    // 处理期间笔记可能已被删除。
                    failedNoteIds.add(note.getId());
                }
            } catch (RuntimeException exception) {
                log.warn("笔记片段生成失败，笔记 ID：{}", note.getId(), exception);
                failedNoteIds.add(note.getId());
            }
        }

        return new BatchEmbeddingResult(
                notes.size(),
                succeeded,
                failedNoteIds.size(),
                failedNoteIds
        );
    }
}