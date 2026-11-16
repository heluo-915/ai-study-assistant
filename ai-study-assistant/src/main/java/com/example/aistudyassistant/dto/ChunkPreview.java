package com.example.aistudyassistant.dto;

// 返回给用户查看的片段，不包含向量。
public record ChunkPreview(
        int chunkIndex,
        String content
) {
}