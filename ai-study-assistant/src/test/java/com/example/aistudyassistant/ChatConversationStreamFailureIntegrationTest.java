package com.example.aistudyassistant;

import com.example.aistudyassistant.service.ChatConversationStreamService;
import com.example.aistudyassistant.service.ChatMessageService;
import com.example.aistudyassistant.service.ChatService;
import com.example.aistudyassistant.service.ChatStreamPersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class ChatConversationStreamFailureIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ChatMessageService messages;
    @Autowired ChatService chat;
    @Autowired ChatStreamPersistenceService persistence;
    private String username;
    private Long userId;
    private Long sessionId;

    // 设置up 字段。
    @BeforeEach
    void setup() {
        username = "stream_failure_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO users(username,password_hash) VALUES (?,?)", username, "test-only");
        userId = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
        jdbc.update("INSERT INTO chat_sessions(user_id,title) VALUES (?,?)", userId, "test");
        sessionId = jdbc.queryForObject("SELECT id FROM chat_sessions WHERE user_id=?", Long.class, userId);
    }

    // 清理本次测试创建的数据。
    @AfterEach
    void cleanup() {
        if (sessionId != null) {
            jdbc.update("DELETE FROM chat_messages WHERE session_id=?", sessionId);
            jdbc.update("DELETE FROM chat_sessions WHERE id=?", sessionId);
        }
        if (username != null) jdbc.update("DELETE FROM users WHERE username=?", username);
    }

    // 验证第二条消息插入失败时第一条消息也会回滚。
    @Test
    void failedSecondInsertRollsBackFirstMessage() {
        // 直接测试保存层的事务边界，绕过流层的长度检查以触发数据库写入失败。
        assertThrows(RuntimeException.class, () -> persistence.save(sessionId, userId, "question", "x".repeat(70000)));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", Integer.class, sessionId));
    }

    // 验证过长的流式内容在写入数据库前被拒绝。
    @Test
    void oversizedStreamIsRejectedBeforePersistence() {
        ChatModel model = mock(ChatModel.class);
        ChatResponse chunk = new ChatResponse(List.of(new Generation(new AssistantMessage("x".repeat(70000)))));
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(chunk));
        var service = new ChatConversationStreamService(messages, chat, model, persistence);
        var events = service.prepare(sessionId, userId, "question").orElseThrow().collectList().block();
        assertNotNull(events);
        assertEquals(List.of("error"), events.stream().map(e -> e.event()).toList());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", Integer.class, sessionId));
    }

    // 验证模型输出片段后失败不会保存本轮消息。
    @Test
    void modelFailureAfterChunkDoesNotSaveEitherMessage() {
        ChatModel model = mock(ChatModel.class);
        ChatResponse chunk = new ChatResponse(List.of(new Generation(new AssistantMessage("partial"))));
        when(model.stream(any(Prompt.class))).thenReturn(Flux.concat(Flux.just(chunk), Flux.error(new IllegalStateException())));
        var service = new ChatConversationStreamService(messages, chat, model, persistence);
        var events = service.prepare(sessionId, userId, "question").orElseThrow().collectList().block();
        assertNotNull(events);
        assertEquals(List.of("message", "error"), events.stream().map(e -> e.event()).toList());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", Integer.class, sessionId));
    }
}
