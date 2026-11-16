package com.example.aistudyassistant.dto;
import java.time.LocalDateTime;
// 同步任务状态快照；版本用于区分运行中的任务和后续修改。
public record NoteVectorSyncTask(Long noteId, Long userId, long requestedVersion, String requestedAction,
                                 Long claimedVersion, String claimedAction, String status,
                                 String errorMessage, int attempts, LocalDateTime nextRetryAt,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {}
