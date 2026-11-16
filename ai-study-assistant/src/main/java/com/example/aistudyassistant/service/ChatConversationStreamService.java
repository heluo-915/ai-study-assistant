package com.example.aistudyassistant.service;

import com.example.aistudyassistant.entity.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.List;
import java.util.Optional;

@Service
public class ChatConversationStreamService {
    private static final Logger log = LoggerFactory.getLogger(ChatConversationStreamService.class);
    public static final int MAX_MESSAGE_BYTES = 65_535;
    private final ChatMessageService messageService;
    private final ChatService chatService;
    private final ChatModel chatModel;
    private final ChatStreamPersistenceService persistenceService;

    public ChatConversationStreamService(ChatMessageService messageService, ChatService chatService,
            ChatModel chatModel, ChatStreamPersistenceService persistenceService) {
        this.messageService = messageService;
        this.chatService = chatService;
        this.chatModel = chatModel;
        this.persistenceService = persistenceService;
    }

    // 在开始 SSE 前检查归属并读取最近 20 条历史。
    public Optional<Flux<ServerSentEvent<String>>> prepare(Long sessionId, Long userId, String question) {
        Optional<List<ChatMessage>> history = messageService.findRecentMessages(sessionId, userId, 20);
        if (history.isEmpty()) return Optional.empty();
        Prompt prompt = chatService.buildPrompt(history.get(), question);
        return Optional.of(Flux.defer(() -> {
            StringBuilder answer = new StringBuilder();
            Utf8LengthCounter byteCounter = new Utf8LengthCounter();
            String[] finishReason = {null};
            return chatModel.stream(prompt)
                    .<ServerSentEvent<String>>handle((response, sink) -> {
                        if (response.getResult() == null) return;
                        String reason = response.getResult().getMetadata().getFinishReason();
                        if (reason != null && !reason.isBlank()) finishReason[0] = reason;
                        if (response.getResult().getOutput() == null) return;
                        String text = response.getResult().getOutput().getText();
                        if (text == null || text.isEmpty()) return;
                        if (byteCounter.append(text) > MAX_MESSAGE_BYTES) {
                            sink.error(new IllegalStateException("模型回答超出数据库容量"));
                            return;
                        }
                        answer.append(text);
                        sink.next(ServerSentEvent.builder(text).event("message").build());
                    })
                    .concatWith(Mono.fromCallable(() -> {
                        if (!"stop".equalsIgnoreCase(finishReason[0])) {
                            throw new IllegalStateException("模型未正常完成回答");
                        }
                        if (answer.toString().isBlank()) throw new IllegalStateException("模型返回空回答");
                        persistenceService.save(sessionId, userId, question, answer.toString());
                        return ServerSentEvent.<String>builder("ok").event("done").build();
                    }).subscribeOn(Schedulers.boundedElastic()));
        }).onErrorResume(error -> {
            log.warn("会话流式回答或保存失败，原因类型：{}", error.getClass().getSimpleName());
            return Flux.just(ServerSentEvent.<String>builder("回答或保存失败，请重试").event("error").build());
        }));
    }
}
