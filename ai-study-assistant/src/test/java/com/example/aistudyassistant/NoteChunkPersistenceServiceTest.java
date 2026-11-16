package com.example.aistudyassistant;

import com.example.aistudyassistant.dto.ChunkEmbedding;
import com.example.aistudyassistant.service.NoteChunkPersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class NoteChunkPersistenceServiceTest {

    @Autowired
    private NoteChunkPersistenceService persistenceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String username;
    private Long userId;
    private Long noteId;

    // 为每个测试创建独立的临时用户和笔记。
    @BeforeEach
    void createTestNote() {
        username = "chunk_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbcTemplate.update("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, "test-only");
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        jdbcTemplate.update("INSERT INTO notes (title, content, user_id) VALUES (?, ?, ?)", "事务测试笔记", "仅用于测试", userId);
        noteId = jdbcTemplate.queryForObject("SELECT id FROM notes WHERE user_id = ?", Long.class, userId);
    }

    // 只清理本次测试创建的数据，片段由外键级联删除。
    @AfterEach
    void cleanupTestData() {
        if (username != null) {
            jdbcTemplate.update("DELETE FROM notes WHERE user_id IN (SELECT id FROM users WHERE username = ?)", username);
            jdbcTemplate.update("DELETE FROM users WHERE username = ?", username);
        }
    }

    // 验证正常情况下两个不同序号的片段均被保存。
    @Test
    void shouldSaveAllChunks() {
        List<ChunkEmbedding> chunks = List.of(
                new ChunkEmbedding(0, "第一段", "[0.1]", "test-model"),
                new ChunkEmbedding(1, "第二段", "[0.2]", "test-model")
        );

        persistenceService.insertChunks(noteId, chunks);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM note_chunks WHERE note_id = ?", Integer.class, noteId);
        assertEquals(2, count);
    }

    // 验证第二次插入违反唯一约束时，第一次插入也会回滚。
    @Test
    void shouldRollbackAllChunksWhenInsertFails() {
        List<ChunkEmbedding> chunks = List.of(
                new ChunkEmbedding(0, "第一段", "[0.1]", "test-model"),
                new ChunkEmbedding(0, "重复序号", "[0.2]", "test-model")
        );

        assertThrows(DataIntegrityViolationException.class,
                () -> persistenceService.insertChunks(noteId, chunks));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM note_chunks WHERE note_id = ?", Integer.class, noteId);
        assertEquals(0, count);
    }
}
