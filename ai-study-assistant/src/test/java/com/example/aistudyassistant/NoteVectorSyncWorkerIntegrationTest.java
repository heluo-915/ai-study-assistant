package com.example.aistudyassistant;

import com.example.aistudyassistant.service.ChunkBuildJobService;
import com.example.aistudyassistant.mapper.NoteVectorSyncTaskMapper;
import com.example.aistudyassistant.service.NoteChunkVectorService;
import com.example.aistudyassistant.service.NoteVectorSyncTaskService;
import com.example.aistudyassistant.service.NoteVectorSyncWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "sync.worker.integration.enabled", matches = "true")
@SpringBootTest
class NoteVectorSyncWorkerIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private NoteVectorSyncTaskService tasks;
    @Autowired private NoteVectorSyncWorker worker;
    @MockitoSpyBean private NoteChunkVectorService vectors;
    @Autowired private NoteVectorSyncTaskMapper mapper;
    @Autowired private TransactionTemplate tx;
    @MockitoBean private ChunkBuildJobService backgroundJobs;

    // 验证重建和删除本人笔记索引时不影响其他用户。
    @Test void reindexesAndDeletesOwnedNoteWithoutTouchingOtherUsers() {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches(
                "jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        assertTrue(System.getProperty("spring.ai.vectorstore.qdrant.collection-name", "")
                .matches("codex_test_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (101,'worker_a','x'),(202,'worker_b','x')");
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES " +
                "(10001,'笔记一','正文',101),(20002,'笔记二','正文',202)");
        jdbc.update("INSERT INTO note_chunks(note_id,chunk_index,content,embedding,model_name) VALUES " +
                "(10001,0,'Java 接口定义方法','[]','test')," +
                "(10001,1,'Java 接口支持多态','[]','test')");
        vectors.saveChunk(202L,20002L,0,"另一位用户的笔记");
        assertTrue(tasks.schedule(101L,10001L,NoteVectorSyncTaskService.Action.UPSERT));
        assertFalse(worker.processPending(202L,10001L));
        assertTrue(worker.processPending(101L,10001L));
        assertEquals("SUCCEEDED",tasks.find(101L,10001L).orElseThrow().status());
        assertEquals(2,vectors.searchChunks(101L,"Java 接口").size());
        assertEquals(1,vectors.searchChunks(202L,"用户笔记").size());

        jdbc.update("DELETE FROM note_chunks WHERE note_id=10001 AND chunk_index=1");
        jdbc.update("UPDATE note_chunks SET content='Java 接口更新内容' WHERE note_id=10001 AND chunk_index=0");
        assertTrue(tasks.schedule(101L,10001L,NoteVectorSyncTaskService.Action.UPSERT));
        assertTrue(worker.processPending(101L,10001L));
        var results=vectors.searchChunks(101L,"Java 接口");
        assertEquals(1,results.size());
        assertEquals("Java 接口更新内容",results.get(0).getText());

        tx.executeWithoutResult(status -> {
            assertTrue(tasks.schedule(101L,10001L,NoteVectorSyncTaskService.Action.DELETE));
            jdbc.update("DELETE FROM notes WHERE id=10001 AND user_id=101");
        });
        assertTrue(worker.processPending(101L,10001L));
        assertEquals("SUCCEEDED",tasks.find(101L,10001L).orElseThrow().status());
        assertTrue(vectors.searchChunks(101L,"Java 接口").isEmpty());
        assertEquals(1,vectors.searchChunks(202L,"用户笔记").size());
    }
    // 验证失败任务仍可重试并能完成。
    @Test void failureRemainsRetryableAndRetryCompletes() {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches(
                "jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        assertTrue(System.getProperty("spring.ai.vectorstore.qdrant.collection-name", "")
                .matches("codex_test_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (303,'worker_failure','x')");
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES (30003,'失败笔记','正文',303)");
        jdbc.update("INSERT INTO note_chunks(note_id,chunk_index,content,embedding,model_name) VALUES " +
                "(30003,0,'可重试的合成片段','[]','test')");
        assertTrue(tasks.schedule(303L,30003L,NoteVectorSyncTaskService.Action.UPSERT));
        doThrow(new IllegalStateException("模拟 Qdrant 失败")).doCallRealMethod()
                .when(vectors).deleteByNote(303L,30003L);
        assertThrows(IllegalStateException.class, () -> worker.processPending(303L,30003L));
        assertEquals("FAILED",tasks.find(303L,30003L).orElseThrow().status());
        assertEquals(1,mapper.retry(303L,30003L));
        assertTrue(worker.processPending(303L,30003L));
        assertEquals("SUCCEEDED",tasks.find(303L,30003L).orElseThrow().status());
        assertEquals(1,vectors.searchChunks(303L,"可重试的合成片段").size());
    }
}
