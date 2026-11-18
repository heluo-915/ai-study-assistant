package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.QuizAnswerRequest;
import com.example.aistudyassistant.dto.QuizAnswerResponse;
import com.example.aistudyassistant.service.QuizAnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import java.util.Set;

@RestController
public class QuizAnswerController {
    private static final Set<String> ANSWERS = Set.of("A", "B", "C", "D");
    private final QuizAnswerService quizAnswerService;

    // 初始化 QuizAnswerController 实例。
    public QuizAnswerController(QuizAnswerService quizAnswerService) { this.quizAnswerService = quizAnswerService; }

    // 只接受 A、B、C、D，题目不存在或不属于当前用户时返回 404。
    @PostMapping("/quiz/{quizId}/answer")
    public ResponseEntity<?> answer(@PathVariable("quizId") Long quizId,
            @RequestAttribute("userId") Long userId, @RequestBody QuizAnswerRequest request) {
        if (request == null || request.answer() == null || !ANSWERS.contains(request.answer())) {
            return ResponseEntity.badRequest().body("答案必须是 A、B、C 或 D");
        }
        Optional<QuizAnswerResponse> result = quizAnswerService.answer(userId, quizId, request.answer());
        return result.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
