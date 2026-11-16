package com.example.aistudyassistant;

import com.example.aistudyassistant.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class EmbeddingServiceTest {
    @Test
    // 验证相同方向向量的余弦相似度为 1。
    void sameDirectionShouldReturnOne() {
        // 准备：创建测试对象和两个方向相同的向量
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        EmbeddingService service = new EmbeddingService(embeddingModel);

        float[] a = {1, 0};
        float[] b = {1, 0};

        // 执行：计算相似度
        double result = service.cosineSimilarity(a, b);

        // 验证：期望结果是 1.0，允许有极小的浮点误差
        assertEquals(1.0, result, 0.000001);
    }
    @Test
    // 验证垂直向量的余弦相似度为 0。
    void perpendicularVectorsShouldReturnZero() {
        EmbeddingModel embeddingModel=mock(EmbeddingModel.class);
        EmbeddingService service=new EmbeddingService(embeddingModel);

        float[] c = {1, 0};
        float[] d = {0, 1};

        double result = service.cosineSimilarity(c, d);

        assertEquals(0, result, 0.000001);

    }

    @Test
    // 验证零向量会被拒绝。
    void zeroVectorShouldThrowException() {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        EmbeddingService service = new EmbeddingService(embeddingModel);

        float[] e = {0, 0};
        float[] f = {1, 0};

        assertThrows(
                IllegalArgumentException.class,
                () -> service.cosineSimilarity(e, f)
        );
    }
    @Test
    // 验证一般向量的余弦计算结果。
    void differentVectorsShouldReturnExpectedSimilarity(){
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        EmbeddingService service = new EmbeddingService(embeddingModel);
        float[] g = {1, 2};
        float[] h = {2, 1};

        double result = service.cosineSimilarity(g, h);

        assertEquals(0.8, result, 0.000001);

    }
}
