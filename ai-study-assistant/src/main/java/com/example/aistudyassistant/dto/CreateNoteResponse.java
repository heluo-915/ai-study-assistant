package com.example.aistudyassistant.dto;

// 笔记创建结果及片段生成状态。
public record CreateNoteResponse(
        Long noteId,
        boolean chunksGenerated,
        String message
) {
}