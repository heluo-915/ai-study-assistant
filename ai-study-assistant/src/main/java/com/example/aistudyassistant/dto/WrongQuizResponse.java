package com.example.aistudyassistant.dto;

import java.util.List;

public record WrongQuizResponse(Long quizId, Long noteId, String question, List<QuizOption> options,
                                boolean latestCorrect) {}
