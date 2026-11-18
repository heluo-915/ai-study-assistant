package com.example.aistudyassistant;

import com.example.aistudyassistant.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatConversationStreamServiceTest {
    // 验证只有正常完成的会话才会保存。
    @Test
    void savesOnlyCompletedConversation() {
        ChatMessageService messages = mock(ChatMessageService.class);
        ChatService chat = mock(ChatService.class);
        ChatModel model = mock(ChatModel.class);
        ChatStreamPersistenceService persistence = mock(ChatStreamPersistenceService.class);
        Prompt prompt = new Prompt("question");
        when(messages.findRecentMessages(8L, 5L, 20)).thenReturn(Optional.of(List.of()));
        when(chat.buildPrompt(List.of(), "question")).thenReturn(prompt);
        var service = new ChatConversationStreamService(messages, chat, model, persistence);
        ChatResponse first = new ChatResponse(List.of(new Generation(new AssistantMessage("one"))));
        ChatResponse second = new ChatResponse(List.of(new Generation(new AssistantMessage("two"),
                ChatGenerationMetadata.builder().finishReason("stop").build())));
        when(model.stream(prompt)).thenReturn(Flux.just(first, second));
        var events = service.prepare(8L, 5L, "question").orElseThrow().collectList().block();
        assertNotNull(events);
        assertEquals(List.of("message", "message", "done"), events.stream().map(e -> e.event()).toList());
        verify(persistence).save(8L, 5L, "question", "onetwo");

        reset(persistence);
        when(model.stream(prompt)).thenReturn(Flux.concat(Flux.just(first), Flux.error(new IllegalStateException())));
        var failed = service.prepare(8L, 5L, "question").orElseThrow().collectList().block();
        assertEquals(List.of("message", "error"), failed.stream().map(e -> e.event()).toList());
        verifyNoInteractions(persistence);

        when(model.stream(prompt)).thenReturn(Flux.just(first, second));
        service.prepare(8L, 5L, "question").orElseThrow().take(1).collectList().block();
        verifyNoInteractions(persistence);

        doThrow(new IllegalStateException()).when(persistence).save(8L, 5L, "question", "onetwo");
        var saveFailed = service.prepare(8L, 5L, "question").orElseThrow().collectList().block();
        assertEquals("error", saveFailed.get(saveFailed.size() - 1).event());
        assertFalse(saveFailed.stream().anyMatch(e -> "done".equals(e.event())));

        reset(persistence);
        when(model.stream(prompt)).thenReturn(Flux.empty());
        var empty = service.prepare(8L, 5L, "question").orElseThrow().collectList().block();
        assertEquals("error", empty.get(0).event());
        verifyNoInteractions(persistence);

        String allowed = "中".repeat(21845);
        ChatResponse boundary = new ChatResponse(List.of(new Generation(new AssistantMessage(allowed),
                ChatGenerationMetadata.builder().finishReason("stop").build())));
        when(model.stream(prompt)).thenReturn(Flux.just(boundary));
        var allowedEvents = service.prepare(8L, 5L, "question").orElseThrow().collectList().block();
        assertEquals("done", allowedEvents.get(allowedEvents.size() - 1).event());
        verify(persistence).save(8L, 5L, "question", allowed);
        reset(persistence);
        ChatResponse excess = new ChatResponse(List.of(new Generation(new AssistantMessage("中"))));
        when(model.stream(prompt)).thenReturn(Flux.just(boundary, excess));
        var tooLong = service.prepare(8L, 5L, "question").orElseThrow().collectList().block();
        assertEquals(List.of("message", "error"), tooLong.stream().map(e -> e.event()).toList());
        verifyNoInteractions(persistence);

        when(model.stream(prompt)).thenReturn(Flux.just(first));
        var noFinish = service.prepare(8L, 5L, "question").orElseThrow().collectList().block();
        assertEquals(List.of("message", "error"), noFinish.stream().map(e -> e.event()).toList());
        verifyNoInteractions(persistence);

        ChatResponse truncated = new ChatResponse(List.of(new Generation(new AssistantMessage("partial"),
                ChatGenerationMetadata.builder().finishReason("length").build())));
        when(model.stream(prompt)).thenReturn(Flux.just(truncated));
        var truncatedEvents = service.prepare(8L, 5L, "question").orElseThrow().collectList().block();
        assertEquals(List.of("message", "error"), truncatedEvents.stream().map(e -> e.event()).toList());
        verifyNoInteractions(persistence);

        assertTrue(service.prepare(999L, 5L, "question").isEmpty());
    }
}
