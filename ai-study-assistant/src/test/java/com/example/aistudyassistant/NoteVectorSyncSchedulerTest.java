package com.example.aistudyassistant;
import com.example.aistudyassistant.dto.NoteVectorSyncTaskKey;
import com.example.aistudyassistant.mapper.NoteVectorSyncTaskMapper;
import com.example.aistudyassistant.service.NoteVectorSyncScheduler;
import com.example.aistudyassistant.service.NoteVectorSyncWorker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class NoteVectorSyncSchedulerTest {
    @Test void dispatchesOnePendingTask() {
        var tasks=mock(NoteVectorSyncTaskMapper.class); var worker=mock(NoteVectorSyncWorker.class);
        when(tasks.nextPending()).thenReturn(new NoteVectorSyncTaskKey(7L,12L));
        new NoteVectorSyncScheduler(tasks,worker).processNext();
        verify(worker).processPending(7L,12L);
    }
    @Test void noTaskDoesNotCallWorker() {
        var tasks=mock(NoteVectorSyncTaskMapper.class); var worker=mock(NoteVectorSyncWorker.class);
        new NoteVectorSyncScheduler(tasks,worker).processNext();
        verifyNoInteractions(worker);
    }
    @Test void workerFailureDoesNotStopNextSchedulerRuns() {
        var tasks=mock(NoteVectorSyncTaskMapper.class); var worker=mock(NoteVectorSyncWorker.class);
        when(tasks.nextPending()).thenReturn(new NoteVectorSyncTaskKey(7L,12L));
        when(worker.processPending(7L,12L)).thenThrow(new IllegalStateException());
        assertDoesNotThrow(() -> new NoteVectorSyncScheduler(tasks,worker).processNext());
    }
}
