package com.example.aistudyassistant;
import com.example.aistudyassistant.entity.NoteChunk;
import com.example.aistudyassistant.mapper.NoteChunkMapper;
import com.example.aistudyassistant.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerifiedQdrantSearchTest {
    private final NoteChunkMapper mapper=mock(NoteChunkMapper.class);
    private final NoteChunkVectorService qdrant=mock(NoteChunkVectorService.class);
    private final EmbeddingService embeddings=mock(EmbeddingService.class);
    private final NoteChunkSearchService search=new NoteChunkSearchService(mapper,mock(NoteEmbeddingService.class),embeddings,qdrant);
    private void enabled() {
        ReflectionTestUtils.setField(search,"modelName","test-model");
        ReflectionTestUtils.setField(search,"qdrantEnabled",true);
    }
    private Document candidate(String user, String text, String model) {
        return Document.builder().id("c8b090e9-5718-4db4-8782-955fd171a214").text(text)
                .metadata(Map.of("userId",user,"noteId","12","chunkIndex",0,"modelName",model)).score(0.9).build();
    }
    @Test void servesOnlyOwnedCurrentContentFromMysql() {
        enabled();
        NoteChunk current=new NoteChunk(); current.setNoteId(12L); current.setNoteTitle("当前标题");
        current.setChunkIndex(0); current.setContent("当前正文"); current.setModelName("test-model");
        when(mapper.countOwnedChunks(7L)).thenReturn(1);
        when(qdrant.countByUser(7L)).thenReturn(1L);
        when(qdrant.searchChunks(7L,"问题")).thenReturn(List.of(candidate("7","当前正文","test-model")));
        when(mapper.findOwnedChunk(7L,12L,0)).thenReturn(current);
        var results=search.search(7L,"问题");
        assertEquals(1,results.size()); assertEquals("当前标题",results.get(0).noteTitle());
        verify(mapper,times(2)).countChunksNotReady(7L,"test-model");
        verify(mapper,never()).findByUserId(anyLong());
    }
    @Test void unsynchronizedIndexFallsBackWithoutQueryingQdrant() {
        enabled(); when(mapper.countChunksNotReady(7L,"test-model")).thenReturn(1);
        when(mapper.findByUserId(7L)).thenReturn(List.of());
        assertTrue(search.search(7L,"问题").isEmpty());
        verifyNoInteractions(qdrant);
    }
    @Test void staleOrForeignContentNeverReturned() {
        enabled(); when(mapper.findByUserId(7L)).thenReturn(List.of());
        when(qdrant.searchChunks(7L,"问题")).thenReturn(List.of(candidate("8","他人内容","test-model")));
        assertTrue(search.search(7L,"问题").isEmpty());
        verify(mapper,never()).findOwnedChunk(anyLong(),anyLong(),anyInt());
    }
    @Test void unavailableIndexFallsBackToMysql() {
        enabled(); when(mapper.findByUserId(7L)).thenReturn(List.of());
        when(qdrant.searchChunks(7L,"问题")).thenThrow(new IllegalStateException());
        assertTrue(search.search(7L,"问题").isEmpty());
        verify(mapper).findByUserId(7L);
    }
    @Test void missingIndexedPointsFallBackEvenWhenOtherPointsExist() {
        enabled(); when(mapper.countOwnedChunks(7L)).thenReturn(2);
        when(qdrant.countByUser(7L)).thenReturn(1L);
        when(mapper.findByUserId(7L)).thenReturn(List.of());
        assertTrue(search.search(7L,"问题").isEmpty());
        verify(qdrant,never()).searchChunks(anyLong(),anyString());
        verify(mapper).findByUserId(7L);
    }
}
