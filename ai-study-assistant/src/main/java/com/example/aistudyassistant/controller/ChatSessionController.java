package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.ChatRequest;
import com.example.aistudyassistant.dto.CreateChatSessionRequest;
import com.example.aistudyassistant.entity.ChatMessage;
import com.example.aistudyassistant.entity.ChatSession;
import com.example.aistudyassistant.service.ChatConversationService;
import com.example.aistudyassistant.service.ChatMessageService;
import com.example.aistudyassistant.service.ChatSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/chat")
public class ChatSessionController {
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ChatConversationService chatConversationService;
    // 通过构造方法接收并保存所需依赖或初始化字段。
    public ChatSessionController(ChatSessionService chatSessionService,
                                 ChatMessageService chatMessageService,
                                 ChatConversationService chatConversationService) {
        this.chatConversationService = chatConversationService;
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;

    }

    @PostMapping("/sessions")
    // 创建会话并保存或返回数据库生成的会话 ID。
    public ResponseEntity<Long> insertChatSession(
            @RequestBody CreateChatSessionRequest request,
            @RequestAttribute("userId") Long userId
    ) {
        String title = request.getTitle();

        if (title == null || title.isBlank() || title.length() > 50) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(chatSessionService.insertChatSession(title, userId));
    }
    @GetMapping("/sessions")
    // 查询指定用户自己的会话列表。
    public List<ChatSession> findChatSessionByUserId(@RequestAttribute("userId") Long userId) {
        return chatSessionService.findChatSessionByUserId(userId);
    }
    @GetMapping("/sessions/{id}")
    // 根据记录 ID 和用户 ID 联合查找，限制数据归属。
    public ResponseEntity<ChatSession> findByIdAndUserId(
            @PathVariable("id") Long id,
            @RequestAttribute("userId") Long userId
    ) {
        ChatSession session = chatSessionService.findByIdAndUserId(id, userId);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(session);
    }
    @GetMapping("/sessions/{sessionId}/messages")
    // 先检查会话归属，再读取会话消息。
    public ResponseEntity<List<ChatMessage>> findMessages(
            @PathVariable("sessionId") Long sessionId,
            @RequestAttribute("userId") Long userId
    ) {
        Optional<List<ChatMessage>> messages =
                chatMessageService.findMessages(sessionId, userId);

        if (messages.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(messages.get());
    }

    @PostMapping("/sessions/{sessionId}/messages")
    // 校验问题并调用会话服务，返回模型回答或 404。
    public ResponseEntity<String> saveConversationTurn(
            @PathVariable("sessionId") Long sessionId,
            @RequestAttribute("userId") Long userId,
            @RequestBody ChatRequest request
    ) {
        String question = request.getMessage();

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<String> answer =
                chatConversationService.ask(sessionId, userId, question);

        if (answer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(answer.get());
    }

}
