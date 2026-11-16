package com.example.aistudyassistant.dto;
import java.time.LocalDateTime;
public record ChunkBuildJob(Long id, Long noteId, String status, int attempts, String errorMessage, LocalDateTime createdAt, LocalDateTime updatedAt) {}
