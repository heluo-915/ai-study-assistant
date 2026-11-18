package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.QuizCreatedResponse;
import com.example.aistudyassistant.service.NoteQuizListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class NoteQuizListController {
    private final NoteQuizListService service;
    // 初始化 NoteQuizListController 实例。
    public NoteQuizListController(NoteQuizListService service) { this.service = service; }

    // 查询当前用户的笔记生成的题目，不展示答案。
    @GetMapping("/notes/{noteId}/quizzes")
    public ResponseEntity<List<QuizCreatedResponse>> list(@PathVariable("noteId") Long noteId,
            @RequestAttribute("userId") Long userId,
            @RequestParam(value = "beforeId", required = false) Long beforeId) {
        if (beforeId != null && beforeId <= 0) return ResponseEntity.badRequest().build();
        return service.list(userId, noteId, beforeId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
