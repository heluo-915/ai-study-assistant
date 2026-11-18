package com.example.aistudyassistant;

import com.example.aistudyassistant.service.NoteChunkVectorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoteChunkVectorServiceTest {
    // 验证写入内容、用户元数据以及稳定且不同的向量 ID。
    @Test
    void savesContentAndOwnerMetadataWithStableDistinctIds() {
        VectorStore store = mock(VectorStore.class);
        NoteChunkVectorService service = new NoteChunkVectorService(store);
        ReflectionTestUtils.setField(service, "modelName", "test-model");

        service.saveChunk(7L, 12L, 0, "第一段");
        service.saveChunk(7L, 12L, 0, "修改后的第一段");
        service.saveChunk(7L, 12L, 1, "第二段");
        service.saveChunk(7L, 13L, 0, "另一篇笔记");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(store, times(4)).add(captor.capture());
        List<List<Document>> calls = captor.getAllValues();
        Document first = calls.get(0).get(0);
        assertEquals("第一段", first.getText());
        assertEquals(7L, first.getMetadata().get("userId"));
        assertEquals(12L, first.getMetadata().get("noteId"));
        assertEquals(0, first.getMetadata().get("chunkIndex"));
        assertEquals(first.getId(), calls.get(1).get(0).getId());
        assertNotEquals(first.getId(), calls.get(2).get(0).getId());
        assertNotEquals(first.getId(), calls.get(3).get(0).getId());
    }
}
