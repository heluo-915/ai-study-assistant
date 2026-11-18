package com.example.aistudyassistant;

import com.example.aistudyassistant.service.ChatStreamPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ChatStreamPersistenceServiceTest {
    @Autowired ChatStreamPersistenceService service;
    @Autowired JdbcTemplate jdbc;

    // 验证只为所属会话保存两条消息。
    @Test
    void savesBothMessagesOnlyForOwnedSession() {
        String username = "stream_test_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO users(username,password_hash) VALUES (?,?)", username, "test-only");
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
        jdbc.update("INSERT INTO chat_sessions(user_id,title) VALUES (?,?)", userId, "test");
        Long sessionId = jdbc.queryForObject("SELECT id FROM chat_sessions WHERE user_id=?", Long.class, userId);
        assertThrows(IllegalStateException.class, () -> service.save(sessionId, userId + 1000000, "q", "a"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", Integer.class, sessionId));
        service.save(sessionId, userId, "question", "answer");
        var rows = jdbc.queryForList("SELECT role, content FROM chat_messages WHERE session_id=? ORDER BY id", sessionId);
        assertEquals(2, rows.size());
        assertEquals("user", rows.get(0).get("role"));
        assertEquals("question", rows.get(0).get("content"));
        assertEquals("assistant", rows.get(1).get("role"));
        assertEquals("answer", rows.get(1).get("content"));
    }
}
