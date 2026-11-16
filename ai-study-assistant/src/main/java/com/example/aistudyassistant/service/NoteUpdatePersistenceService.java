package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.ChunkEmbedding;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.mapper.NoteMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Objects;

@Service
public class NoteUpdatePersistenceService {
    private final NoteMapper notes;
    private final NoteEmbeddingService embeddings;
    private final NoteChunkPersistenceService chunks;
    public NoteUpdatePersistenceService(NoteMapper notes, NoteEmbeddingService embeddings,
                                        NoteChunkPersistenceService chunks) {
        this.notes=notes; this.embeddings=embeddings; this.chunks=chunks;
    }

    // 锁定并核验原始快照，在一个短事务内替换正文、向量、片段及同步任务。
    @Transactional
    public int replace(Long userId, Long noteId, String oldTitle, String oldContent,
                       String title, String content, String vector, List<ChunkEmbedding> prepared) {
        Note current=notes.findByIdAndUserIdForUpdate(userId,noteId);
        if (current==null) return 0;
        if (!Objects.equals(oldTitle,current.getTitle()) || !Objects.equals(oldContent,current.getContent()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"笔记已发生变化，请刷新后重试");
        if (notes.updateNoteByIdAndUserId(title,content,noteId,userId)!=1)
            throw new IllegalStateException("笔记更新失败");
        embeddings.savePreparedEmbedding(noteId,vector);
        chunks.insertChunksIfUnchanged(userId,noteId,content,prepared);
        return 1;
    }
}
