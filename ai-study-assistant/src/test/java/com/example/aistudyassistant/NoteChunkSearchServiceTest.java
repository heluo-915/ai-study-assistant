package com.example.aistudyassistant;

import com.example.aistudyassistant.entity.NoteChunk;
import com.example.aistudyassistant.mapper.NoteChunkMapper;
import com.example.aistudyassistant.service.EmbeddingService;
import com.example.aistudyassistant.service.NoteChunkSearchService;
import com.example.aistudyassistant.service.NoteEmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoteChunkSearchServiceTest {
    @Test
    void returnsUpToTenRelevantChunksInScoreOrderForCurrentUser() {
        NoteChunkMapper mapper = mock(NoteChunkMapper.class);
        NoteEmbeddingService parser = mock(NoteEmbeddingService.class);
        EmbeddingService embeddings = mock(EmbeddingService.class);
        NoteChunkSearchService search = new NoteChunkSearchService(mapper, parser, embeddings, mock(com.example.aistudyassistant.service.NoteChunkVectorService.class));
        ReflectionTestUtils.setField(search, "modelName", "test-model");
        List<NoteChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            NoteChunk chunk = new NoteChunk();
            chunk.setNoteId((long) i + 1);
            chunk.setNoteTitle("笔记" + i);
            chunk.setContent("内容" + i);
            chunk.setModelName("test-model");
            chunk.setEmbedding(Integer.toString(i));
            chunks.add(chunk);
            float[] vector = {(float) i};
            when(parser.parseEmbedding(Integer.toString(i))).thenReturn(vector);
            when(embeddings.cosineSimilarity(any(), same(vector))).thenReturn(0.6 + i * 0.01);
        }
        when(mapper.findByUserId(42L)).thenReturn(chunks);
        when(embeddings.embed("问题")).thenReturn(new float[]{1});

        var result = search.search(42L, "问题");

        assertEquals(10, result.size());
        assertEquals(12L, result.get(0).noteId());
        assertEquals(3L, result.get(9).noteId());
        verify(mapper).findByUserId(42L);
    }

    @Test
    void returnsEmptyWithoutEmbeddingIfUserHasNoChunks() {
        NoteChunkMapper mapper = mock(NoteChunkMapper.class);
        EmbeddingService embeddings = mock(EmbeddingService.class);
        when(mapper.findByUserId(42L)).thenReturn(List.of());
        var search = new NoteChunkSearchService(mapper, mock(NoteEmbeddingService.class), embeddings, mock(com.example.aistudyassistant.service.NoteChunkVectorService.class));

        assertTrue(search.search(42L, "问题").isEmpty());
        verifyNoInteractions(embeddings);
    }
}
