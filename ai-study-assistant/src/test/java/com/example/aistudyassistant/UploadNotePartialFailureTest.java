package com.example.aistudyassistant;

import com.example.aistudyassistant.controller.NoteController;
import com.example.aistudyassistant.dto.CreateNoteResponse;
import com.example.aistudyassistant.service.EmbeddingService;
import com.example.aistudyassistant.service.NoteChunkBuildService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class UploadNotePartialFailureTest {
    @Autowired private NoteController controller;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private EmbeddingService embeddingService;
    @MockitoBean private NoteChunkBuildService chunkBuildService;
    private String username;
    private Long userId;

    @BeforeEach
    void createUser() {
        username = "upload_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbcTemplate.update("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, "test-only");
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    @AfterEach
    void cleanup() {
        if (username != null) {
            jdbcTemplate.update("DELETE FROM notes WHERE user_id IN (SELECT id FROM users WHERE username = ?)", username);
            jdbcTemplate.update("DELETE FROM users WHERE username = ?", username);
        }
    }

    // 片段生成失败时，返回已创建的笔记 ID 并保留笔记和整篇向量。
    @Test
    void shouldKeepNoteWhenChunkGenerationFails() throws Exception {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(chunkBuildService.buildChunks(eq(userId), anyLong()))
                .thenThrow(new IllegalStateException("模拟片段生成失败"));
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain",
                "测试笔记内容".getBytes(StandardCharsets.UTF_8));

        ResponseEntity<?> response = controller.upload(file, "上传测试", userId);

        assertEquals(201, response.getStatusCode().value());
        CreateNoteResponse body = assertInstanceOf(CreateNoteResponse.class, response.getBody());
        assertNotNull(body.noteId());
        assertFalse(body.chunksGenerated());
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE id = ? AND user_id = ?", Integer.class, body.noteId(), userId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_embeddings WHERE note_id = ?", Integer.class, body.noteId()));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_chunks WHERE note_id = ?", Integer.class, body.noteId()));
    }

    // 整篇向量生成失败时，创建笔记的事务回滚且不尝试生成片段。
    @Test
    void shouldRollbackNoteWhenInitialEmbeddingFails() {
        when(embeddingService.embed(anyString())).thenThrow(new IllegalStateException("模拟向量生成失败"));
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain",
                "测试笔记内容".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> controller.upload(file, "上传测试", userId));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE user_id = ?", Integer.class, userId));
        verifyNoInteractions(chunkBuildService);
    }
}
