package com.example.aistudyassistant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class QdrantConnectionTest {

    @Autowired
    private VectorStore vectorStore;

    @Test
    void shouldSaveAndFindDocument() {
        // 给这次测试生成唯一标记，避免匹配到其他数据
        String testRun = UUID.randomUUID().toString();

        Document document = new Document(
                "Java 接口用于定义类需要实现的行为",
                Map.of("testRun", testRun)
        );

        try {
            // 保存文本：Spring AI 会调用 Ollama 生成向量，再写入 Qdrant
            vectorStore.add(List.of(document));

            // 搜索时只查找带本次测试标记的数据
            SearchRequest request = SearchRequest.builder()
                    .query("Java 接口用于定义类需要实现的行为")
                    .topK(5)
                    .filterExpression("testRun == '" + testRun + "'")
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            // 检查搜索结果中是否包含刚才保存的文档
            assertTrue(
                    results.stream()
                            .anyMatch(result -> document.getId().equals(result.getId()))
            );
        } finally {
            // 测试结束后删除本次创建的文档
            vectorStore.delete(List.of(document.getId()));
        }
    }
}