package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.QuizAttemptResponse;
import com.example.aistudyassistant.service.QuizAttemptHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class QuizAttemptHistoryController {
    private final QuizAttemptHistoryService service;
    // 初始化 QuizAttemptHistoryController 实例。
    public QuizAttemptHistoryController(QuizAttemptHistoryService service) { this.service = service; }

    // 返回本人某道题最近 50 次作答。
    @GetMapping("/quiz/{quizId}/attempts")
    public ResponseEntity<List<QuizAttemptResponse>> history(@PathVariable("quizId") Long quizId,
            @RequestAttribute("userId") Long userId,
            @RequestParam(value = "beforeId", required = false) Long beforeId) {
        if (beforeId != null && beforeId <= 0) return ResponseEntity.badRequest().build();
        return service.findHistory(userId, quizId, beforeId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
