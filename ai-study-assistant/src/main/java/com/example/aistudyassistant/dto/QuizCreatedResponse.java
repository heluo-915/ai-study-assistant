package com.example.aistudyassistant.dto;

import java.util.List;

// 出题时只展示题目与选项，答案留给后续作答接口。
public record QuizCreatedResponse(Long quizId, String question, List<QuizOption> options) {}
