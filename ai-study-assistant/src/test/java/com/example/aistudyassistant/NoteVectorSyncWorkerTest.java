package com.example.aistudyassistant;

import com.example.aistudyassistant.dto.ChunkPreview;
import com.example.aistudyassistant.dto.NoteVectorSyncTask;
import com.example.aistudyassistant.mapper.NoteVectorSyncTaskMapper;
import com.example.aistudyassistant.service.NoteChunkPreviewService;
import com.example.aistudyassistant.service.NoteChunkVectorService;
import com.example.aistudyassistant.service.NoteVectorSyncWorker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NoteVectorSyncWorkerTest {
    private final NoteVectorSyncTaskMapper tasks = mock(NoteVectorSyncTaskMapper.class);
    private final NoteChunkPreviewService previews = mock(NoteChunkPreviewService.class);
    private final NoteChunkVectorService vectors = mock(NoteChunkVectorService.class);
    private final NoteVectorSyncWorker worker = new NoteVectorSyncWorker(tasks, previews, vectors);

    private NoteVectorSyncTask task(String action) {
        return new NoteVectorSyncTask(12L, 7L, 1L, action, 1L, action, "RUNNING", null, 1, null, null, null);
    }
    private void claim(String action) {
        when(tasks.claim(eq(7L), eq(12L), anyString())).thenReturn(1);
        when(tasks.find(7L, 12L)).thenReturn(task(action));
        when(tasks.finish(eq(7L), eq(12L), anyString(), anyBoolean())).thenReturn(1);
    }

    @Test void noClaimNeverTouchesVectors() {
        assertFalse(worker.processPending(7L, 12L));
        verifyNoInteractions(previews, vectors);
    }
    @Test void upsertReadsOwnedSnapshotBeforeReplacing() {
        claim("UPSERT");
        when(previews.findPreviews(7L, 12L)).thenReturn(Optional.of(List.of(
                new ChunkPreview(0, "第一段"), new ChunkPreview(1, "第二段"))));
        assertTrue(worker.processPending(7L, 12L));
        var order = inOrder(previews, vectors);
        order.verify(previews).findPreviews(7L, 12L);
        order.verify(vectors).deleteByNote(7L, 12L);
        order.verify(vectors).saveChunk(7L, 12L, 0, "第一段");
        order.verify(vectors).saveChunk(7L, 12L, 1, "第二段");
        verify(tasks).finish(eq(7L), eq(12L), anyString(), eq(true));
    }
    @Test void deletionDoesNotRequireAnExistingNote() {
        claim("DELETE");
        assertTrue(worker.processPending(7L, 12L));
        verify(vectors).deleteByNote(7L, 12L);
        verifyNoInteractions(previews);
    }
    @Test void foreignOrMissingNoteFailsBeforeDeleting() {
        claim("UPSERT");
        when(previews.findPreviews(7L, 12L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> worker.processPending(7L, 12L));
        verifyNoInteractions(vectors);
        verify(tasks).finish(eq(7L), eq(12L), anyString(), eq(false));
    }
    @Test void writeFailureIsPropagatedAndRecorded() {
        claim("UPSERT");
        when(previews.findPreviews(7L, 12L)).thenReturn(Optional.of(List.of(new ChunkPreview(0, "片段"))));
        doThrow(new IllegalStateException("模拟失败")).when(vectors).saveChunk(7L, 12L, 0, "片段");
        assertThrows(IllegalStateException.class, () -> worker.processPending(7L, 12L));
        verify(tasks).finish(eq(7L), eq(12L), anyString(), eq(false));
    }
    @Test void failureToUpdateFinalStateIsNotReportedAsSuccess() {
        claim("DELETE");
        when(tasks.finish(eq(7L), eq(12L), anyString(), eq(true))).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> worker.processPending(7L, 12L));
    }
}
