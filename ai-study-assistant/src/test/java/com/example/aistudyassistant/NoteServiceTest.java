package com.example.aistudyassistant;
import com.example.aistudyassistant.dto.BatchEmbeddingResult;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.mapper.NoteMapper;
import com.example.aistudyassistant.mapper.NoteChunkMapper;
import com.example.aistudyassistant.service.NoteEmbeddingService;
import com.example.aistudyassistant.service.NoteService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
class NoteServiceTest {
    @Test
    void rebuildMissingEmbeddingsShouldContinueAfterFailure() {
        NoteMapper noteMapper = mock(NoteMapper.class);
        NoteEmbeddingService embeddingService = mock(NoteEmbeddingService.class);
        NoteChunkMapper noteChunkMapper = mock(NoteChunkMapper.class);
        NoteService service = new NoteService(noteMapper, embeddingService, noteChunkMapper, mock(com.example.aistudyassistant.service.NoteVectorSyncTaskService.class), mock(com.example.aistudyassistant.service.NoteUpdateService.class));

        Note first = new Note();
        first.setId(1L);
        first.setContent("第一篇");

        Note second = new Note();
        second.setId(2L);
        second.setContent("第二篇");

        Note third = new Note();
        third.setId(3L);
        third.setContent("第三篇");

        when(noteMapper.findMissingEmbeddingByUserId(10L))
                .thenReturn(List.of(first, second, third));

        // 模拟第二篇生成向量时发生异常
        when(embeddingService.updateEmbedding(2L, "第二篇"))
                .thenThrow(new RuntimeException("模拟生成失败"));

        BatchEmbeddingResult result = service.rebuildMissingEmbeddings(10L);

        assertEquals(3, result.total());
        assertEquals(2, result.succeeded());
        assertEquals(1, result.failed());
        assertEquals(List.of(2L), result.failedNoteIds());

        // 确认第二篇失败后，第三篇仍然被处理
        verify(embeddingService).updateEmbedding(3L, "第三篇");
    }
}