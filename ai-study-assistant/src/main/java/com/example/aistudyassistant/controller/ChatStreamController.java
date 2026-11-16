package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.ChatRequest;
import com.example.aistudyassistant.service.ChatStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatStreamController {
    private final ChatStreamService service;
    public ChatStreamController(ChatStreamService service) { this.service = service; }

    // 提交一条问题，返回 SSE 文本片段；不读写会话历史。
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> stream(@RequestBody ChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(service.stream(request.getMessage()));
    }
}
