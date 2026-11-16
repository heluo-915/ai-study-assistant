package com.example.aistudyassistant.dto;

// PDF 提取文本的预览结果。
public record PdfPreviewResponse(
        String preview,
        int totalCodePoints,
        boolean truncated
) {
}