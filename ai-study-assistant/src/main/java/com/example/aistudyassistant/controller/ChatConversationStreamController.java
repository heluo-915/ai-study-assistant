package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.ChatRequest;
import com.example.aistudyassistant.service.ChatConversationStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.nio.charset.StandardCharsets;

@RestController
public class ChatConversationStreamController {
    private final ChatConversationStreamService service;
    public ChatConversationStreamController(ChatConversationStreamService service) { this.service = service; }

    // 单轮问题以 SSE 返回，成功保存后发送 done。
    @PostMapping(value = "/chat/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> stream(@PathVariable("sessionId") Long sessionId,
            @RequestAttribute("userId") Long userId, @RequestBody ChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getMessage().getBytes(StandardCharsets.UTF_8).length > ChatConversationStreamService.MAX_MESSAGE_BYTES) {
            return ResponseEntity.badRequest().build();
        }
        return service.prepare(sessionId, userId, request.getMessage())
                .map(events -> ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(events))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
