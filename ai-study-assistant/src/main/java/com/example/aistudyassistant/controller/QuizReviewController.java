package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.QuizCreatedResponse;
import com.example.aistudyassistant.service.QuizReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuizReviewController {
    private final QuizReviewService service;
    public QuizReviewController(QuizReviewService service) { this.service = service; }

    // 重练时查看当前用户自己的题目，不返回答案和解析。
    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<QuizCreatedResponse> findQuestion(@PathVariable("quizId") Long quizId,
            @RequestAttribute("userId") Long userId) {
        return service.findQuestion(userId, quizId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
