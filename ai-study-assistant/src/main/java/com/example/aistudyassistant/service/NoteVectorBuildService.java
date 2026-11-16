package com.example.aistudyassistant.service;
import com.example.aistudyassistant.entity.Note;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Arrays;
@Service
public class NoteVectorBuildService {
    private final NoteService notes;
    private final EmbeddingService embedding;
    private final NoteVectorPersistenceService persistence;
    @Value("${spring.ai.ollama.embedding.model}") private String model;
    public NoteVectorBuildService(NoteService notes, EmbeddingService embedding, NoteVectorPersistenceService persistence) {
        this.notes=notes; this.embedding=embedding; this.persistence=persistence;
    }
    public void build(Long userId, Long noteId) {
        Note note=notes.findByIdAndUserId(userId,noteId);
        if (note==null) throw new IllegalStateException("笔记不存在");
        String content=note.getContent();
        String vector=Arrays.toString(embedding.embed(content));
        persistence.saveIfUnchanged(userId,noteId,content,vector,model);
    }
}
