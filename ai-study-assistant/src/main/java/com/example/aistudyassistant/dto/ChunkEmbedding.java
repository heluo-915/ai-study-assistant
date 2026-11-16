package com.example.aistudyassistant.dto;

// 一段文本及其生成的向量。
public record ChunkEmbedding(
        int chunkIndex,
        String content,
        String embedding,
        String modelName
) {
}