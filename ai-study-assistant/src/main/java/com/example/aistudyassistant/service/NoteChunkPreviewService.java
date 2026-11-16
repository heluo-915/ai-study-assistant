package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.ChunkPreview;
import com.example.aistudyassistant.mapper.NoteChunkMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NoteChunkPreviewService {

    private final NoteService noteService;
    private final NoteChunkMapper noteChunkMapper;

    public NoteChunkPreviewService(
            NoteService noteService,
            NoteChunkMapper noteChunkMapper
    ) {
        this.noteService = noteService;
        this.noteChunkMapper = noteChunkMapper;
    }

    // 查询当前用户指定笔记的片段预览。
    public Optional<List<ChunkPreview>> findPreviews(Long userId, Long noteId) {
        if (noteService.findByIdAndUserId(userId, noteId) == null) {
            return Optional.empty();
        }

        List<ChunkPreview> chunks =
                noteChunkMapper.findPreviewsByNoteIdAndUserId(noteId, userId);

        return Optional.of(chunks);
    }
}