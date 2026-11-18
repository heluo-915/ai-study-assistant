package com.example.aistudyassistant;

import com.example.aistudyassistant.mapper.NoteVectorSyncTaskMapper;
import com.example.aistudyassistant.service.ChunkBuildJobService;
import com.example.aistudyassistant.service.NoteVectorSyncTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@EnabledIfSystemProperty(named="sync.tasks.integration.enabled", matches="true")
@SpringBootTest
class NoteVectorSyncTaskDatabaseTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private NoteVectorSyncTaskMapper mapper;
    @Autowired private NoteVectorSyncTaskService service;
    @Autowired private TransactionTemplate transaction;
    @MockitoBean private ChunkBuildJobService backgroundJobs;

    // 验证同步任务的归属、版本、失败重试和删除记录。
    @Test void recordsOwnershipVersionsFailureRetryAndDeletionTombstone() {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches(
                "jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (101,'job_a','x'),(202,'job_b','x')");
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES (10001,'笔记','正文',101),(10002,'回滚','正文',101)");
        assertFalse(service.schedule(202L,10001L,NoteVectorSyncTaskService.Action.UPSERT));
        assertTrue(service.find(202L,10001L).isEmpty());
        assertTrue(service.schedule(101L,10001L,NoteVectorSyncTaskService.Action.UPSERT));
        assertTrue(service.schedule(101L,10001L,NoteVectorSyncTaskService.Action.UPSERT));
        var pending = service.find(101L,10001L).orElseThrow();
        assertEquals(2,pending.requestedVersion());
        assertEquals("PENDING",pending.status());
        assertEquals(1,mapper.claim(101L,10001L,"worker-a"));
        assertEquals(0,mapper.claim(101L,10001L,"worker-b"));
        assertTrue(service.schedule(101L,10001L,NoteVectorSyncTaskService.Action.DELETE));
        var running = service.find(101L,10001L).orElseThrow();
        assertEquals(3,running.requestedVersion());
        assertEquals(2L,running.claimedVersion());
        assertEquals("DELETE",running.requestedAction());
        assertEquals("UPSERT",running.claimedAction());
        assertEquals(0,mapper.finish(101L,10001L,"wrong-token",true));
        assertEquals(1,mapper.finish(101L,10001L,"worker-a",true));
        assertEquals("PENDING",service.find(101L,10001L).orElseThrow().status());
        assertEquals(1,mapper.claim(101L,10001L,"worker-b"));
        assertEquals(1,mapper.finish(101L,10001L,"worker-b",false));
        assertEquals("FAILED",service.find(101L,10001L).orElseThrow().status());
        assertEquals(0,mapper.retry(202L,10001L));
        assertEquals(1,mapper.retry(101L,10001L));
        jdbc.update("DELETE FROM notes WHERE id=10001 AND user_id=101");
        assertEquals("PENDING",service.find(101L,10001L).orElseThrow().status());
        assertEquals("DELETE",service.find(101L,10001L).orElseThrow().requestedAction());

        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            service.schedule(101L,10002L,NoteVectorSyncTaskService.Action.UPSERT);
            throw new IllegalStateException("模拟事务回滚");
        }));
        assertTrue(service.find(101L,10002L).isEmpty());
    }
    // 验证并发领取和手动恢复使用预期令牌。
    @Test void concurrentClaimAndManualRecoveryUseTheExpectedToken() throws Exception {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches(
                "jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (303,'recovery_owner','x'),(404,'recovery_other','x')");
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES (30003,'恢复测试','正文',303)");
        assertTrue(service.schedule(303L,30003L,NoteVectorSyncTaskService.Action.UPSERT));
        UUID a=UUID.randomUUID(), b=UUID.randomUUID();
        var gate=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        int ra, rb;
        try {
            var fa=pool.submit(() -> { gate.await(); return mapper.claim(303L,30003L,a.toString()); });
            var fb=pool.submit(() -> { gate.await(); return mapper.claim(303L,30003L,b.toString()); });
            gate.countDown();
            ra=fa.get(10,TimeUnit.SECONDS); rb=fb.get(10,TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        assertEquals(1,ra+rb);
        UUID winning = ra == 1 ? a : b;
        UUID losing = ra == 1 ? b : a;
        assertFalse(service.recoverAfterWorkerStopped(404L,30003L,winning));
        assertFalse(service.recoverAfterWorkerStopped(303L,30003L,losing));
        assertEquals("RUNNING",service.find(303L,30003L).orElseThrow().status());
        // 此处仅模拟旧工作进程已退出；测试本身不启动任何向量工作进程。
        assertTrue(service.recoverAfterWorkerStopped(303L,30003L,winning));
        assertEquals("PENDING",service.find(303L,30003L).orElseThrow().status());
        assertFalse(service.recoverAfterWorkerStopped(303L,30003L,winning));
        UUID next=UUID.randomUUID();
        assertEquals(1,mapper.claim(303L,30003L,next.toString()));
        assertEquals(0,mapper.finish(303L,30003L,winning.toString(),true));
        assertEquals("RUNNING",service.find(303L,30003L).orElseThrow().status());
        assertEquals(1,mapper.finish(303L,30003L,next.toString(),true));
        assertEquals("SUCCEEDED",service.find(303L,30003L).orElseThrow().status());
    }
}
