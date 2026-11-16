package com.example.aistudyassistant.dto;

// 更新后的笔记 ID 和片段生成状态。
public record UpdateNoteResponse(
        Long noteId,
        boolean chunksGenerated,
        String message
) {
}