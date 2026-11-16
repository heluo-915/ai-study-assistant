package com.example.aistudyassistant.dto;

// 提供给模型参考的笔记片段来源。
public record ChunkSource(
        Long noteId,
        String noteTitle,
        int chunkIndex
) {
}