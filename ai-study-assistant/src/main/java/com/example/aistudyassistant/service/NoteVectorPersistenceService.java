package com.example.aistudyassistant.service;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.mapper.NoteEmbeddingMapper;
import com.example.aistudyassistant.mapper.NoteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
@Service
public class NoteVectorPersistenceService {
    private final NoteMapper notes;
    private final NoteEmbeddingMapper embeddings;
    public NoteVectorPersistenceService(NoteMapper notes, NoteEmbeddingMapper embeddings) { this.notes=notes; this.embeddings=embeddings; }
    @Transactional
    public void saveIfUnchanged(Long userId, Long noteId, String content, String json, String model) {
        Note current=notes.findByIdAndUserIdForUpdate(userId,noteId);
        if (current==null || !Objects.equals(content,current.getContent())) throw new IllegalStateException("笔记不存在或已变更");
        embeddings.upsert(noteId,json,model);
    }
}
