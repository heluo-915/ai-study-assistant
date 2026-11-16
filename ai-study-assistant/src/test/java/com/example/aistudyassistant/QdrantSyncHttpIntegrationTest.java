package com.example.aistudyassistant;

import com.example.aistudyassistant.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="qdrant.http.integration.enabled",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"app.qdrant-search.enabled=true","app.qdrant-sync.enabled=true","app.qdrant-sync.poll-ms=300"})
class QdrantSyncHttpIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtService jwt;
    @Autowired private EmbeddingService embeddings;
    @Autowired private NoteVectorSyncTaskService tasks;
    @MockitoBean private ChunkBuildJobService unrelatedJobs;
    @MockitoSpyBean private NoteChunkVectorService vectorStore;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private HttpResponse<String> call(String method,String path,String token,String body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(90));
        if (token!=null) builder.header("Authorization","Bearer "+token);
        if (body!=null) builder.header("Content-Type","application/json");
        return client.send(builder.method(method,body==null ? HttpRequest.BodyPublishers.noBody() :
                HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private void awaitTask(long user,long note,String action) throws Exception { awaitTask(user,note,action,false); }
    private void awaitTask(long user,long note,String action,boolean allowFailed) throws Exception {
        for (int i=0;i<120;i++) {
            var state=tasks.find(user,note);
            if (state.isPresent() && action.equals(state.get().requestedAction())) {
                if (!allowFailed) assertNotEquals("FAILED",state.get().status());
                if ("SUCCEEDED".equals(state.get().status())) return;
            }
            Thread.sleep(500);
        }
        fail("任务未在时间内完成");
    }

    @Test void oldNotesBackfillUpdateDeleteAndIsolationThroughRealHttp() throws Exception {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches("jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        assertTrue(System.getProperty("spring.ai.vectorstore.qdrant.collection-name", "").matches("codex_test_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (901,'http_a','x'),(902,'http_b','x')");
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES (91001,'HTTP 笔记','原始正文',901)");
        String content="Java 接口定义了类需要实现的方法";
        String vector=Arrays.toString(embeddings.embed(content));
        jdbc.update("INSERT INTO note_chunks(note_id,chunk_index,content,embedding,model_name) VALUES (91001,0,?,?,'qwen3-embedding:0.6b')",content,vector);
        String a=jwt.generateToken(901L), b=jwt.generateToken(902L);
        assertEquals(401,call("POST","/notes/vector-sync/backfill",null,null).statusCode());
        assertEquals(202,call("POST","/notes/vector-sync/backfill",a,null).statusCode());
        assertEquals(404,call("GET","/notes/91001/vector-sync",b,null).statusCode());
        awaitTask(901,91001,"UPSERT");
        String q=URLEncoder.encode(content,StandardCharsets.UTF_8);
        var own=call("GET","/notes/chunks/search?question="+q,a,null);
        assertEquals(200,own.statusCode()); assertTrue(own.body().contains(content));
        var foreign=call("GET","/notes/chunks/search?question="+q,b,null);
        assertEquals(200,foreign.statusCode()); assertFalse(foreign.body().contains(content));
        String update="{\"title\":\"更新标题\",\"content\":\"更新后的学习资料用于练习接口\"}";
        assertEquals(200,call("PUT","/notes/91001",a,update).statusCode());
        awaitTask(901,91001,"UPSERT");
        var updated=call("GET","/notes/chunks/search?question="+
                URLEncoder.encode("更新后的学习资料用于练习接口",StandardCharsets.UTF_8),a,null);
        assertEquals(200,updated.statusCode()); assertTrue(updated.body().contains("更新后的学习资料"));
        assertFalse(updated.body().contains(content));
        assertEquals(204,call("DELETE","/notes/91001",a,null).statusCode());
        awaitTask(901,91001,"DELETE");
        assertFalse(call("GET","/notes/chunks/search?question="+q,a,null).body().contains(content));
        // 模拟一次向量服务故障，检查退避和自动重试。
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES (92002,'重试笔记','正文',902)");
        jdbc.update("INSERT INTO note_chunks(note_id,chunk_index,content,embedding,model_name) VALUES (92002,0,?,?,'qwen3-embedding:0.6b')",content,vector);
        doThrow(new IllegalStateException("模拟短暂故障")).doCallRealMethod().when(vectorStore).deleteByNote(902L,92002L);
        assertEquals(202,call("POST","/notes/92002/vector-sync",b,null).statusCode());
        boolean failed=false;
        for (int i=0;i<120;i++) {
            var state=tasks.find(902L,92002L);
            if (state.isPresent() && "FAILED".equals(state.get().status())) { failed=true; break; }
            Thread.sleep(250);
        }
        assertTrue(failed,"第一次故障应进入 FAILED");
        assertEquals(1,jdbc.queryForObject("SELECT attempts FROM note_vector_sync_tasks WHERE note_id=92002",Integer.class));
        assertNotNull(jdbc.queryForObject("SELECT next_retry_at FROM note_vector_sync_tasks WHERE note_id=92002",java.time.LocalDateTime.class));
        jdbc.update("UPDATE note_vector_sync_tasks SET next_retry_at=DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 SECOND) WHERE note_id=92002");
        awaitTask(902,92002,"UPSERT",true);
        assertEquals(2,jdbc.queryForObject("SELECT attempts FROM note_vector_sync_tasks WHERE note_id=92002",Integer.class));

    }
}
