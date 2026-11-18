package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.ChunkEmbedding;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.mapper.NoteChunkMapper;
import com.example.aistudyassistant.mapper.NoteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class NoteChunkPersistenceService {

    private final NoteChunkMapper noteChunkMapper;
    private final NoteMapper noteMapper;
    private final NoteVectorSyncTaskService vectorSyncTasks;

    // 初始化 NoteChunkPersistenceService 实例。
    public NoteChunkPersistenceService(NoteChunkMapper noteChunkMapper, NoteMapper noteMapper, NoteVectorSyncTaskService vectorSyncTasks) {
        this.noteChunkMapper = noteChunkMapper;
        this.noteMapper = noteMapper;
        this.vectorSyncTasks = vectorSyncTasks;
    }

    // 在同一事务中保存一篇笔记的所有新片段。
    @Transactional
    public void insertChunks(Long noteId, List<ChunkEmbedding> chunks) {
        for (ChunkEmbedding chunk : chunks) {
            noteChunkMapper.insert(
                    noteId,
                    chunk.chunkIndex(),
                    chunk.content(),
                    chunk.embedding(),
                    chunk.modelName()
            );
        }
    }
    // 锁定笔记并确认正文未变化，再保存新片段。
    @Transactional
    public void insertChunksIfUnchanged(
            Long userId,
            Long noteId,
            String originalContent,
            List<ChunkEmbedding> chunks
    ) {
        Note note = noteMapper.findByIdAndUserIdForUpdate(userId, noteId);

        if (note == null) {
            throw new IllegalStateException("笔记不存在或不属于当前用户");
        }

        if (!Objects.equals(note.getContent(), originalContent)) {
            throw new IllegalStateException("笔记内容已变化，请重新生成片段");
        }

        noteChunkMapper.deleteByNoteId(noteId);
        insertChunks(noteId, chunks);
        if (!vectorSyncTasks.schedule(userId, noteId, NoteVectorSyncTaskService.Action.UPSERT))
            throw new IllegalStateException("无法登记片段同步任务");
    }
}