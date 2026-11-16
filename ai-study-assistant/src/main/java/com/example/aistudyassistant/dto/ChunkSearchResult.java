package com.example.aistudyassistant.dto;

// 一条命中的笔记片段及其相似度。
public record ChunkSearchResult(
        Long noteId,
        String noteTitle,
        int chunkIndex,
        String content,
        double score
) {
}