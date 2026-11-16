package com.example.aistudyassistant.controller;
import com.example.aistudyassistant.dto.ChatRequest;
import com.example.aistudyassistant.service.StudyAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class StudyAssistantController {
    private final StudyAssistantService service;
    public StudyAssistantController(StudyAssistantService service) { this.service=service; }
    @PostMapping("/study/assistant")
    public ResponseEntity<String> ask(@RequestAttribute("userId") Long userId, @RequestBody ChatRequest request) {
        String question=request.getMessage();
        if (question == null || question.isBlank() || question.length()>2000)
            return ResponseEntity.badRequest().body("问题不能为空且不能超过2000个字符");
        return ResponseEntity.ok(service.ask(userId, question));
    }
}
