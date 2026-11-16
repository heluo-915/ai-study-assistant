package com.example.aistudyassistant;

import com.example.aistudyassistant.dto.ChunkPreview;
import com.example.aistudyassistant.service.NoteChunkPreviewService;
import com.example.aistudyassistant.service.NoteChunkSyncService;
import com.example.aistudyassistant.service.NoteChunkVectorService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoteChunkSyncServiceTest {
    private final NoteChunkPreviewService previews = mock(NoteChunkPreviewService.class);
    private final NoteChunkVectorService vectors = mock(NoteChunkVectorService.class);
    private final NoteChunkSyncService service = new NoteChunkSyncService(previews, vectors);

    @Test void writesEveryOwnedChunkAndReturnsCount() {
        when(previews.findPreviews(7L, 12L)).thenReturn(Optional.of(List.of(
                new ChunkPreview(0, "第一段"), new ChunkPreview(1, "第二段"))));
        assertEquals(2, service.copyExistingChunks(7L, 12L).orElseThrow());
        var order = inOrder(vectors);
        order.verify(vectors).saveChunk(7L, 12L, 0, "第一段");
        order.verify(vectors).saveChunk(7L, 12L, 1, "第二段");
        verifyNoMoreInteractions(vectors);
    }

    @Test void missingOrForeignNoteNeverWritesVectors() {
        when(previews.findPreviews(7L, 12L)).thenReturn(Optional.empty());
        assertTrue(service.copyExistingChunks(7L, 12L).isEmpty());
        verifyNoInteractions(vectors);
    }

    @Test void emptyOwnedNoteReturnsZeroWithoutWrites() {
        when(previews.findPreviews(7L, 12L)).thenReturn(Optional.of(List.of()));
        assertEquals(0, service.copyExistingChunks(7L, 12L).orElseThrow());
        verifyNoInteractions(vectors);
    }

    @Test void failedWriteStopsAndDoesNotReportSuccess() {
        when(previews.findPreviews(7L, 12L)).thenReturn(Optional.of(List.of(
                new ChunkPreview(0, "第一段"), new ChunkPreview(1, "第二段"), new ChunkPreview(2, "第三段"))));
        doThrow(new IllegalStateException("模拟写入失败")).when(vectors).saveChunk(7L, 12L, 1, "第二段");
        assertThrows(IllegalStateException.class, () -> service.copyExistingChunks(7L, 12L));
        verify(vectors).saveChunk(7L, 12L, 0, "第一段");
        verify(vectors, never()).saveChunk(7L, 12L, 2, "第三段");
    }
}
