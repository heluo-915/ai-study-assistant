package com.example.aistudyassistant;

import com.example.aistudyassistant.mapper.NoteEmbeddingMapper;
import com.example.aistudyassistant.service.EmbeddingService;
import com.example.aistudyassistant.service.NoteEmbeddingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;

class NoteEmbeddingServiceTest {

    // 验证 JSON 数组能转换为 float 数组。
    @Test
    void parseEmbeddingShouldReturnFloatArray() {
        NoteEmbeddingMapper mapper = mock(NoteEmbeddingMapper.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        NoteEmbeddingService service = new NoteEmbeddingService(mapper, embeddingService);

        String embeddingJson = "[0.1, 0.2, 0.3]";

        float[] result = service.parseEmbedding(embeddingJson);

        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, result, 0.000001f);
    }
}