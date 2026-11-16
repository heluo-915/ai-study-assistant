package com.example.aistudyassistant;
import com.example.aistudyassistant.mapper.ChunkBuildJobMapper;
import com.example.aistudyassistant.service.ChunkBuildJobService;
import com.example.aistudyassistant.service.NoteChunkBuildService;
import com.example.aistudyassistant.service.NoteVectorBuildService;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChunkBuildJobServiceTest {
    @Test void successfulBuildCompletesTheClaimedJob() {
        var mapper=mock(ChunkBuildJobMapper.class); var build=mock(NoteChunkBuildService.class);
        var service=new ChunkBuildJobService(mapper,build,mock(NoteVectorBuildService.class));
        when(mapper.nextId()).thenReturn(9L); when(mapper.claim(eq(9L),anyString())).thenReturn(1);
        when(mapper.owner(eq(9L),anyString())).thenReturn(7L); when(mapper.note(eq(9L),anyString())).thenReturn(3L);
        when(build.buildChunks(7L,3L)).thenReturn(true);
        service.processNext();
        verify(build).buildChunks(7L,3L); verify(mapper).finish(eq(9L),anyString(),eq("SUCCEEDED"));
    }
    @Test void failedBuildIsMarkedFailedAndCanBeRetried() {
        var mapper=mock(ChunkBuildJobMapper.class); var build=mock(NoteChunkBuildService.class);
        var service=new ChunkBuildJobService(mapper,build,mock(NoteVectorBuildService.class));
        when(mapper.nextId()).thenReturn(9L); when(mapper.claim(eq(9L),anyString())).thenReturn(1);
        when(mapper.owner(eq(9L),anyString())).thenReturn(7L); when(mapper.note(eq(9L),anyString())).thenReturn(3L);
        when(build.buildChunks(7L,3L)).thenThrow(new IllegalStateException());
        service.processNext();
        verify(mapper).finish(eq(9L),anyString(),eq("FAILED"));
    }
    @Test void anotherWorkerClaimPreventsDuplicateProcessing() {
        var mapper=mock(ChunkBuildJobMapper.class); var build=mock(NoteChunkBuildService.class);
        when(mapper.nextId()).thenReturn(9L); when(mapper.claim(eq(9L),anyString())).thenReturn(0);
        new ChunkBuildJobService(mapper,build,mock(NoteVectorBuildService.class)).processNext();
        verifyNoInteractions(build);
    }
}
