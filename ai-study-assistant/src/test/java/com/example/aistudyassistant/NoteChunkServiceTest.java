package com.example.aistudyassistant;

import com.example.aistudyassistant.dto.ChunkEmbedding;
import com.example.aistudyassistant.service.EmbeddingService;
import com.example.aistudyassistant.service.NoteChunkService;
import com.example.aistudyassistant.service.TextChunkService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteChunkServiceTest {

    @Test
    void generateChunksShouldEmbedEachChunkInOrder() {
        TextChunkService textChunkService = mock(TextChunkService.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);

        NoteChunkService service =
                new NoteChunkService(textChunkService, embeddingService);

        // 单元测试没有启动 Spring，需要手动设置 @Value 字段。
        ReflectionTestUtils.setField(service, "modelName", "test-model");

        when(textChunkService.split("原始文本", 500, 50))
                .thenReturn(List.of("第一段", "第二段"));
        when(embeddingService.embed("第一段"))
                .thenReturn(new float[]{0.1f, 0.2f});
        when(embeddingService.embed("第二段"))
                .thenReturn(new float[]{0.3f, 0.4f});

        List<ChunkEmbedding> result = service.generateChunks("原始文本");

        assertEquals(2, result.size());
        assertEquals(0, result.get(0).chunkIndex());
        assertEquals("第一段", result.get(0).content());
        assertEquals("[0.1, 0.2]", result.get(0).embedding());
        assertEquals("test-model", result.get(0).modelName());

        assertEquals(1, result.get(1).chunkIndex());
        assertEquals("第二段", result.get(1).content());
        assertEquals("[0.3, 0.4]", result.get(1).embedding());

        verify(embeddingService).embed("第一段");
        verify(embeddingService).embed("第二段");
    }
    @Test
    void generateChunksShouldStopWhenEmbeddingFails() {
        TextChunkService textChunkService = mock(TextChunkService.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);

        NoteChunkService service =
                new NoteChunkService(textChunkService, embeddingService);

        when(textChunkService.split("原始文本", 500, 50))
                .thenReturn(List.of("第一段", "第二段", "第三段"));

        when(embeddingService.embed("第一段"))
                .thenReturn(new float[]{0.1f});
        when(embeddingService.embed("第二段"))
                .thenThrow(new RuntimeException("模拟向量生成失败"));

        assertThrows(
                RuntimeException.class,
                () -> service.generateChunks("原始文本")
        );

        verify(embeddingService, never()).embed("第三段");
    }
}