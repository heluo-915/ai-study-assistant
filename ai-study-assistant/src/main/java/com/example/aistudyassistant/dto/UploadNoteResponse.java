package com.example.aistudyassistant.dto;

// 上传创建的笔记，以及片段是否生成成功。
public record UploadNoteResponse(
        Long noteId,
        boolean chunksGenerated,
        String message
) {
}