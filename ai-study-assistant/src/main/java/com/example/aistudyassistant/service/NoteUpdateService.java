package com.example.aistudyassistant.service;

import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.mapper.NoteMapper;
import org.springframework.stereotype.Service;

// 在事务外准备向量和片段，失败时不修改已有笔记。
@Service
public class NoteUpdateService {
    private final NoteMapper notes;
    private final NoteEmbeddingService embeddings;
    private final NoteChunkService chunks;
    private final NoteUpdatePersistenceService persistence;

    /* 保存笔记查询、向量准备和事务写入所需的依赖。 */ public NoteUpdateService(NoteMapper notes, NoteEmbeddingService embeddings,
                             NoteChunkService chunks, NoteUpdatePersistenceService persistence) {
        this.notes=notes; this.embeddings=embeddings; this.chunks=chunks; this.persistence=persistence;
    }

    /* 先读取本人笔记并准备新向量和片段，再交由持久化服务校验更新。 */ public int update(Long userId, Long noteId, String title, String content) {
        Note snapshot=notes.findByIdAndUserId(userId,noteId);
        if (snapshot==null) return 0;
        String vector=embeddings.prepareEmbedding(content);
        var prepared=chunks.generateChunks(content);
        return persistence.replace(userId,noteId,snapshot.getTitle(),snapshot.getContent(),title,content,vector,prepared);
    }
}
