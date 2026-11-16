package com.example.aistudyassistant.dto;

import java.util.List;

// 记录一次批量补建的处理结果。
public record BatchEmbeddingResult(
        int total,
        int succeeded,
        int failed,
        List<Long> failedNoteIds
) {
}