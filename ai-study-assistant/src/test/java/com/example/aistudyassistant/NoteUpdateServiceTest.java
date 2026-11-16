package com.example.aistudyassistant;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.mapper.NoteMapper;
import com.example.aistudyassistant.service.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoteUpdateServiceTest {
    private final NoteMapper notes=mock(NoteMapper.class);
    private final NoteEmbeddingService embeddings=mock(NoteEmbeddingService.class);
    private final NoteChunkService chunks=mock(NoteChunkService.class);
    private final NoteUpdatePersistenceService persistence=mock(NoteUpdatePersistenceService.class);
    private final NoteUpdateService service=new NoteUpdateService(notes,embeddings,chunks,persistence);

    @Test void missingNoteDoesNotCallModels() {
        assertEquals(0,service.update(1L,2L,"标题","正文"));
        verifyNoInteractions(embeddings,chunks,persistence);
    }
    @Test void modelFailureDoesNotPersist() {
        Note note=new Note(); note.setTitle("旧标题"); note.setContent("旧正文");
        when(notes.findByIdAndUserId(1L,2L)).thenReturn(note);
        when(embeddings.prepareEmbedding("新正文")).thenReturn("[1]");
        when(chunks.generateChunks("新正文")).thenThrow(new IllegalStateException("模型故障"));
        assertThrows(IllegalStateException.class,() -> service.update(1L,2L,"新标题","新正文"));
        verifyNoInteractions(persistence);
    }
    @Test void preparedDataAndOriginalSnapshotReachPersistence() {
        Note note=new Note(); note.setTitle("旧标题"); note.setContent("旧正文");
        when(notes.findByIdAndUserId(1L,2L)).thenReturn(note);
        when(embeddings.prepareEmbedding("新正文")).thenReturn("[1]");
        when(chunks.generateChunks("新正文")).thenReturn(List.of());
        when(persistence.replace(1L,2L,"旧标题","旧正文","新标题","新正文","[1]",List.of())).thenReturn(1);
        assertEquals(1,service.update(1L,2L,"新标题","新正文"));
    }
}
