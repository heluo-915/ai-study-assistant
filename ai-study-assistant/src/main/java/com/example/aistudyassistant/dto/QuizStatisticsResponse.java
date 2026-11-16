package com.example.aistudyassistant.dto;

// 按作答次数统计，重复作答同一题也分别计数。
public record QuizStatisticsResponse(long totalAttempts, long correctAttempts, long incorrectAttempts) {}
