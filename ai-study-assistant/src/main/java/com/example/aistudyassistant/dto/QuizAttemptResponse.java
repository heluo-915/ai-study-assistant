package com.example.aistudyassistant.dto;

import java.time.LocalDateTime;

// 一次作答的选项、结果和时间。
public record QuizAttemptResponse(Long attemptId, String selectedAnswer, boolean correct, LocalDateTime createdAt) {}
