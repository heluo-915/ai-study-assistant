package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.ChatRequest;
import com.example.aistudyassistant.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatService chatService;
    // 通过构造方法接收并保存所需依赖或初始化字段。
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    @PostMapping("/chat")
    // 校验用户消息并调用单轮对话。
    public ResponseEntity<String> ask(@RequestBody ChatRequest chatRequest){
        String message =chatRequest.getMessage();
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body("问题不能为空");
        }
        String answer=chatService.ask(message);
        return ResponseEntity.ok(answer);

    }
}
