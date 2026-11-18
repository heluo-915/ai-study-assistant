package com.example.aistudyassistant;

import com.example.aistudyassistant.dto.ChunkEmbedding;
import com.example.aistudyassistant.mapper.NoteVectorSyncTaskMapper;
import com.example.aistudyassistant.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named = "sync.enqueue.integration.enabled", matches = "true")
@SpringBootTest
class NoteVectorTaskEnqueueIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private NoteChunkPersistenceService chunks;
    @Autowired private NoteService notes;
    @Autowired private NoteVectorSyncTaskMapper taskMapper;
    @MockitoSpyBean private NoteVectorSyncTaskService sync;
    @MockitoBean private NoteEmbeddingService embeddings;
    @MockitoBean private NoteChunkService preparedChunks;
    @MockitoBean private ChunkBuildJobService jobs;

    // 验证片段变更和笔记删除时同步登记任务。
    @Test void chunkChangesAndDeletionCommitWithTasks() {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches(
                "jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (701,'enqueue_a','x'),(702,'enqueue_b','x')");
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES (71001,'测试','旧正文',701)");
        chunks.insertChunksIfUnchanged(701L,71001L,"旧正文",List.of(new ChunkEmbedding(0,"片段","[]","test")));
        assertEquals("UPSERT",sync.find(701L,71001L).orElseThrow().requestedAction());
        assertEquals(0,notes.deleteByIdAndUserId(702L,71001L));
        when(embeddings.prepareEmbedding("新正文")).thenReturn("[]");
        when(preparedChunks.generateChunks("新正文")).thenReturn(List.of(new ChunkEmbedding(0,"新片段","[]","test")));
        assertEquals(1,notes.updateNoteByIdAndUserId("测试","新正文",71001L,701L));
        assertEquals("UPSERT",sync.find(701L,71001L).orElseThrow().requestedAction());
        chunks.insertChunksIfUnchanged(701L,71001L,"新正文",List.of(new ChunkEmbedding(0,"新片段","[]","test")));
        assertEquals("UPSERT",sync.find(701L,71001L).orElseThrow().requestedAction());
        assertEquals(1,notes.deleteByIdAndUserId(701L,71001L));
        assertEquals("DELETE",sync.find(701L,71001L).orElseThrow().requestedAction());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM notes WHERE id=71001",Integer.class));
    }

    // 验证任务登记失败会回滚笔记更新。
    @Test void failingToRecordTaskRollsBackNoteUpdate() {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches(
                "jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (703,'enqueue_fail','x')");
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES (73003,'测试','原文',703)");
        when(embeddings.prepareEmbedding("修改")).thenReturn("[]");
        when(preparedChunks.generateChunks("修改")).thenReturn(List.of(new ChunkEmbedding(0,"新片段","[]","test")));
        doThrow(new IllegalStateException("模拟任务登记失败")).when(sync)
                .schedule(703L,73003L,NoteVectorSyncTaskService.Action.UPSERT);
        assertThrows(IllegalStateException.class,() -> notes.updateNoteByIdAndUserId("测试","修改",73003L,703L));
        assertEquals("原文",jdbc.queryForObject("SELECT content FROM notes WHERE id=73003",String.class));
    }
    // 验证回填按用户限定范围且不重置已有任务。
    @Test void backfillIsScopedAndDoesNotResetExistingTasks() {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches(
                "jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (704,'backfill_a','x'),(705,'backfill_b','x')");
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES " +
                "(74001,'有片段','a',704),(74002,'无片段','b',704),(75001,'他人','c',705)");
        jdbc.update("INSERT INTO note_chunks(note_id,chunk_index,content,embedding,model_name) VALUES " +
                "(74001,0,'片段','[]','test'),(75001,0,'他人片段','[]','test')");
        sync.scheduleMissing(704L);
        assertEquals(1,sync.find(704L,74001L).orElseThrow().requestedVersion());
        assertTrue(sync.find(704L,74002L).isEmpty());
        assertTrue(sync.find(705L,75001L).isEmpty());
        sync.scheduleMissing(704L);
        assertEquals(1,sync.find(704L,74001L).orElseThrow().requestedVersion());
    }
    // 验证重试具有间隔和次数限制，并可手动重置。
    @Test void retriesAreDelayedBoundedAndCanBeResetManually() {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches(
                "jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (706,'retry_test','x')");
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES (76001,'重试','正文',706)");
        assertTrue(sync.schedule(706L,76001L,NoteVectorSyncTaskService.Action.UPSERT));
        for (int attempt=1;attempt<=3;attempt++) {
            if (attempt>1) jdbc.update("UPDATE note_vector_sync_tasks SET next_retry_at=DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 SECOND) WHERE note_id=76001");
            String token="retry-"+attempt;
            assertEquals(1,taskMapper.claim(706L,76001L,token));
            assertEquals(1,taskMapper.finish(706L,76001L,token,false));
            assertEquals("FAILED",sync.find(706L,76001L).orElseThrow().status());
            assertEquals(attempt,jdbc.queryForObject("SELECT attempts FROM note_vector_sync_tasks WHERE note_id=76001",Integer.class));
            if (attempt<3) assertEquals(0,taskMapper.claim(706L,76001L,"too-early"));
        }
        jdbc.update("UPDATE note_vector_sync_tasks SET next_retry_at=DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 SECOND) WHERE note_id=76001");
        assertEquals(0,taskMapper.claim(706L,76001L,"exhausted"));
        assertEquals(1,taskMapper.retry(706L,76001L));
        assertEquals(1,taskMapper.claim(706L,76001L,"manual"));
        assertEquals(1,taskMapper.finish(706L,76001L,"manual",true));
    }
}
