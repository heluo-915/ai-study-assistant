package com.example.aistudyassistant.dto;

import java.util.List;

// 基于笔记片段的回答及参考来源。
public record ChunkAnswerResponse(
        String answer,
        List<ChunkSource> sources
) {
}