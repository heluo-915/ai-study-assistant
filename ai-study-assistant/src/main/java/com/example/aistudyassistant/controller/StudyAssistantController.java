package com.example.aistudyassistant.controller;
import com.example.aistudyassistant.dto.ChatRequest;
import com.example.aistudyassistant.service.StudyAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class StudyAssistantController {
    private final StudyAssistantService service;
    // 初始化 StudyAssistantController 实例。
    public StudyAssistantController(StudyAssistantService service) { this.service=service; }
    // 校验请求并调用当前用户的学习助手。
    @PostMapping("/study/assistant")
    public ResponseEntity<String> ask(@RequestAttribute("userId") Long userId, @RequestBody ChatRequest request) {
        String question=request.getMessage();
        if (question == null || question.isBlank() || question.length()>2000)
            return ResponseEntity.badRequest().body("问题不能为空且不能超过2000个字符");
        return ResponseEntity.ok(service.ask(userId, question));
    }
}
