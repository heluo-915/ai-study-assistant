package com.example.aistudyassistant.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common.Filter;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NoteChunkVectorService {

    @Value("${spring.ai.ollama.embedding.model}")
    private String modelName;
    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    private final VectorStore vectorStore;

    /* 保存向量存储客户端，供片段索引操作使用。 */ public NoteChunkVectorService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // 把一个笔记片段写入 Qdrant
    public void saveChunk(
            Long userId,
            Long noteId,
            int chunkIndex,
            String content
    ) {
        // 用笔记 ID 和片段序号生成稳定的向量记录 ID
        String vectorId = buildVectorId(noteId, chunkIndex);

        Document document = new Document(
                vectorId,
                content,
                Map.of(
                        "userId", userId,
                        "noteId", noteId,
                        "chunkIndex", chunkIndex,
                        "modelName", modelName
                )
        );

        vectorStore.add(List.of(document));
    }
    // 搜索当前用户的相关笔记片段
    public List<Document> searchChunks(Long userId, String question) {
        FilterExpressionBuilder filter = new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(10)
                .filterExpression(
                        filter.eq("userId", userId.toString()).build()
                )
                .build();

        return vectorStore.similaritySearch(request);
    }
    // 根据笔记 ID 和片段序号生成稳定的向量记录 ID
    private String buildVectorId(Long noteId, int chunkIndex) {
        String source = "note-chunk:" + noteId + ":" + chunkIndex;

        return UUID.nameUUIDFromBytes(
                source.getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
    // 删除指定笔记片段对应的向量
    public void deleteChunk(Long noteId, int chunkIndex) {
        String vectorId = buildVectorId(noteId, chunkIndex);
        vectorStore.delete(List.of(vectorId));
    }
    // 删除当前用户指定笔记的所有片段向量
    public void deleteByNote(Long userId, Long noteId) {
        FilterExpressionBuilder filter = new FilterExpressionBuilder();

        vectorStore.delete(
                filter.and(
                        filter.eq("userId", userId.toString()),
                        filter.eq("noteId", noteId.toString())
                ).build()
        );
    }

    // 使用精确计数检查当前用户索引数量；不返回或遍历其他用户的数据。
    public long countByUser(Long userId) {
        QdrantClient client = vectorStore.<QdrantClient>getNativeClient()
                .orElseThrow(() -> new IllegalStateException("无法获取 Qdrant 客户端"));
        Filter filter = Filter.newBuilder()
                .addMust(ConditionFactory.matchKeyword("userId", userId.toString())).build();
        try {
            return client.countAsync(collectionName, filter, true, Duration.ofSeconds(5))
                    .get(6, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("索引计数被中断", interrupted);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException failure) {
            throw new IllegalStateException("索引计数失败", failure);
        }
    }
}
