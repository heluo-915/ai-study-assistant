package com.example.aistudyassistant;
import com.example.aistudyassistant.dto.ChunkEmbedding;
import com.example.aistudyassistant.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named="note.atomic.integration.enabled",matches="true")
@SpringBootTest
class NoteAtomicUpdateIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private NoteService notes;
    @MockitoBean private EmbeddingService embeddings;
    @MockitoBean private ChunkBuildJobService jobs;
    @MockitoSpyBean private NoteVectorSyncTaskService sync;
    @MockitoSpyBean private NoteChunkService chunks;

    // 准备测试所需的初始数据。
    private void seed(long user,long note,String name) {
        assertTrue(System.getenv("SPRING_DATASOURCE_URL").matches("jdbc:mysql://127\\.0\\.0\\.1:3307/codex_sync_[0-9a-f]{16}"));
        jdbc.update("INSERT INTO users(id,username,password_hash) VALUES (?,?,'x')",user,name);
        jdbc.update("INSERT INTO notes(id,title,content,user_id) VALUES (?,'旧标题','旧正文',?)",note,user);
        jdbc.update("INSERT INTO note_chunks(note_id,chunk_index,content,embedding,model_name) VALUES (?,0,'旧片段','[1]','test')",note);
    }
    // 验证模型失败时保留原笔记和原片段。
    @Test void modelFailureKeepsOriginalNoteAndChunks() {
        seed(901,91001,"atomic_failure");
        when(embeddings.embed("新正文")).thenThrow(new IllegalStateException("模拟模型故障"));
        assertThrows(IllegalStateException.class,() -> notes.updateNoteByIdAndUserId("新标题","新正文",91001L,901L));
        assertEquals("旧正文",jdbc.queryForObject("SELECT content FROM notes WHERE id=91001",String.class));
        assertEquals("旧片段",jdbc.queryForObject("SELECT content FROM note_chunks WHERE note_id=91001",String.class));
    }
    // 验证成功更新时同时替换片段并登记索引任务。
    @Test void successfulUpdateAtomicallyReplacesChunksAndQueuesIndex() {
        seed(902,92002,"atomic_success");
        when(embeddings.embed("新正文")).thenReturn(new float[]{1,0});
        assertEquals(1,notes.updateNoteByIdAndUserId("新标题","新正文",92002L,902L));
        assertEquals("新正文",jdbc.queryForObject("SELECT content FROM notes WHERE id=92002",String.class));
        assertEquals("新正文",jdbc.queryForObject("SELECT content FROM note_chunks WHERE note_id=92002",String.class));
        assertEquals("UPSERT",sync.find(902L,92002L).orElseThrow().requestedAction());
    }
    // 验证同步任务登记失败会回滚笔记及片段修改。
    @Test void enqueueFailureRollsBackNoteAndChunks() {
        seed(903,93003,"atomic_rollback");
        when(embeddings.embed("新正文")).thenReturn(new float[]{1,0});
        doThrow(new IllegalStateException("任务写入失败")).when(sync)
                .schedule(903L,93003L,NoteVectorSyncTaskService.Action.UPSERT);
        assertThrows(IllegalStateException.class,() -> notes.updateNoteByIdAndUserId("新标题","新正文",93003L,903L));
        assertEquals("旧正文",jdbc.queryForObject("SELECT content FROM notes WHERE id=93003",String.class));
        assertEquals("旧片段",jdbc.queryForObject("SELECT content FROM note_chunks WHERE note_id=93003",String.class));
    }
    // 验证并发修改不会被旧快照覆盖。
    @Test void concurrentChangeIsNotOverwritten() {
        seed(904,94004,"atomic_conflict");
        when(embeddings.embed("新正文")).thenReturn(new float[]{1,0});
        doAnswer(invocation -> {
            jdbc.update("UPDATE notes SET content='并发修改' WHERE id=94004");
            return List.of(new ChunkEmbedding(0,"新正文","[1,0]","test"));
        }).when(chunks).generateChunks("新正文");
        var error=assertThrows(ResponseStatusException.class,() -> notes.updateNoteByIdAndUserId("新标题","新正文",94004L,904L));
        assertEquals(409,error.getStatusCode().value());
        assertEquals("并发修改",jdbc.queryForObject("SELECT content FROM notes WHERE id=94004",String.class));
        assertEquals("旧片段",jdbc.queryForObject("SELECT content FROM note_chunks WHERE note_id=94004",String.class));
    }
}
