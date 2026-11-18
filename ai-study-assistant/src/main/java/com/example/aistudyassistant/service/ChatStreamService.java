package com.example.aistudyassistant.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ChatStreamService {
    private final ChatModel chatModel;
    // 初始化 ChatStreamService 实例。
    public ChatStreamService(ChatModel chatModel) { this.chatModel = chatModel; }

    // 模型文本逐段发送；正常完成与失败使用不同事件，避免客户端误判。
    public Flux<ServerSentEvent<String>> stream(String message) {
        return Flux.defer(() -> {
            String[] finishReason = {null};
            return chatModel.stream(new Prompt(message))
                .<ServerSentEvent<String>>handle((ChatResponse response, reactor.core.publisher.SynchronousSink<ServerSentEvent<String>> sink) -> {
                    if (response.getResult() != null) {
                        String reason = response.getResult().getMetadata().getFinishReason();
                        if (reason != null && !reason.isBlank()) finishReason[0] = reason;
                        if (response.getResult().getOutput() == null) return;
                        String text = response.getResult().getOutput().getText();
                        if (text != null && !text.isEmpty()) {
                            sink.next(ServerSentEvent.builder(text).event("message").build());
                        }
                    }
                })
                .concatWith(Mono.defer(() -> "stop".equalsIgnoreCase(finishReason[0])
                        ? Mono.just(ServerSentEvent.<String>builder("ok").event("done").build())
                        : Mono.error(new IllegalStateException("模型未正常完成回答"))));
        }).onErrorResume(error -> Flux.just(ServerSentEvent.<String>builder("模型服务暂时不可用，请重试").event("error").build()));
    }
}
