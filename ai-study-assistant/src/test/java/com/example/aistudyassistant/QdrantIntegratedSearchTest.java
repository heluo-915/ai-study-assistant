package com.example.aistudyassistant;
import com.example.aistudyassistant.dto.ChunkEmbedding;
import com.example.aistudyassistant.mapper.NoteChunkMapper;
import com.example.aistudyassistant.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.*;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="qdrant.complete.integration.enabled",matches="true")
@SpringBootTest(properties="app.qdrant-search.enabled=true")
class QdrantIntegratedSearchTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private NoteChunkPersistenceService chunks;
    @Autowired private NoteChunkSearchService search;
    @Autowired private NoteVectorSyncWorker worker;
    @Autowired private NoteVectorSyncTaskService tasks;
    @Autowired private NoteService notes;
    @Autowired private EmbeddingService embeddings;
    @MockitoBean private ChunkBuildJobService jobs;
    @MockitoSpyBean private NoteChunkVectorService vectors;
    @MockitoSpyBean private NoteChunkMapper chunkMapper;

    @Test void validatesRealIndexAndFallsBackWhenNewChangesArePending() {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches(
                "jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        assertTrue(System.getProperty("spring.ai.vectorstore.qdrant.collection-name", "")
                .matches("codex_test_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (801,'index_a','x'),(802,'index_b','x')");
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES (81001,'原始标题','原始正文',801)");
        String text="Java 接口定义类需要实现的方法";
        String json=Arrays.toString(embeddings.embed(text));
        chunks.insertChunksIfUnchanged(801L,81001L,"原始正文",List.of(
                new ChunkEmbedding(0,text,json,"qwen3-embedding:0.6b")));
        // 任务未处理时走 MySQL，不能返回其他用户数据。
        assertEquals(1,search.search(801L,text).size());
        assertTrue(search.search(802L,text).isEmpty());
        assertTrue(worker.processPending(801L,81001L));
        assertEquals("SUCCEEDED",tasks.find(801L,81001L).orElseThrow().status());
        clearInvocations(chunkMapper);
        var indexed=search.search(801L,text);
        assertEquals(1,indexed.size()); assertEquals("原始标题",indexed.get(0).noteTitle());
        verify(vectors, atLeastOnce()).searchChunks(801L,text);
        verify(chunkMapper, never()).findByUserId(801L);
        assertTrue(search.search(802L,text).isEmpty());
        // 模拟索引点丢失：数量不一致时，仍从 MySQL 找到正文。
        vectors.deleteChunk(81001L,0);
        clearInvocations(chunkMapper);
        assertEquals(1,search.search(801L,text).size());
        verify(chunkMapper).findByUserId(801L);
        assertTrue(tasks.schedule(801L,81001L,NoteVectorSyncTaskService.Action.UPSERT));
        assertTrue(worker.processPending(801L,81001L));
        // 更新后数据库片段立即切换，新索引就绪前回退 MySQL。
        assertEquals(1,notes.updateNoteByIdAndUserId("新标题","更新正文",81001L,801L));
        var fallback=search.search(801L,"更新正文");
        assertEquals(1,fallback.size());
        assertEquals("更新正文",fallback.get(0).content());
        assertTrue(worker.processPending(801L,81001L));
        assertEquals("更新正文",search.search(801L,"更新正文").get(0).content());
    }
}
