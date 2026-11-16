package com.example.aistudyassistant;

import com.example.aistudyassistant.service.ChatStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatStreamServiceTest {
    @Test
    void emitsChunksAndDoneOnlyOnSuccess() {
        ChatModel model = mock(ChatModel.class);
        ChatResponse first = new ChatResponse(java.util.List.of(new Generation(new AssistantMessage("第一段"))));
        ChatResponse second = new ChatResponse(java.util.List.of(new Generation(new AssistantMessage("第二段"),
                ChatGenerationMetadata.builder().finishReason("stop").build())));
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(first, second));
        var events = new ChatStreamService(model).stream("问题").collectList().block();
        assertNotNull(events);
        assertEquals(3, events.size());
        assertEquals("message", events.get(0).event());
        assertEquals("第一段", events.get(0).data());
        assertEquals("第二段", events.get(1).data());
        assertEquals("done", events.get(2).event());
    }

    @Test
    void unfinishedOutputMustNotSendDone() {
        ChatModel model = mock(ChatModel.class);
        ChatResponse partial = new ChatResponse(java.util.List.of(new Generation(new AssistantMessage("part"))));
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(partial));
        var events = new ChatStreamService(model).stream("问题").collectList().block();
        assertNotNull(events);
        assertEquals(java.util.List.of("message", "error"), events.stream().map(e -> e.event()).toList());
    }

    @Test
    void emitsErrorWithoutDoneAfterFailure() {
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenReturn(Flux.error(new IllegalStateException("private details")));
        var events = new ChatStreamService(model).stream("问题").collectList().block();
        assertNotNull(events);
        assertEquals(1, events.size());
        assertEquals("error", events.get(0).event());
        assertFalse(events.get(0).data().contains("private details"));
    }
}
