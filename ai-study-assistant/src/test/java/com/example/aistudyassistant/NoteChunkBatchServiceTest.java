package com.example.aistudyassistant;

import com.example.aistudyassistant.dto.BatchEmbeddingResult;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.mapper.NoteMapper;
import com.example.aistudyassistant.service.NoteChunkBatchService;
import com.example.aistudyassistant.service.NoteChunkBuildService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NoteChunkBatchServiceTest {

    // 一篇生成失败、另一篇已删除时，仍继续处理后续笔记并统计失败 ID。
    @Test
    void shouldContinueAfterFailures() {
        NoteMapper mapper = mock(NoteMapper.class);
        NoteChunkBuildService builder = mock(NoteChunkBuildService.class);
        NoteChunkBatchService service = new NoteChunkBatchService(mapper, builder);

        Note first = new Note(); first.setId(1L);
        Note second = new Note(); second.setId(2L);
        Note third = new Note(); third.setId(3L);
        Note fourth = new Note(); fourth.setId(4L);
        when(mapper.findMissingChunksByUserId(10L)).thenReturn(List.of(first, second, third, fourth));
        when(builder.buildChunks(10L, 1L)).thenReturn(true);
        when(builder.buildChunks(10L, 2L)).thenThrow(new IllegalStateException("模拟生成失败"));
        when(builder.buildChunks(10L, 3L)).thenReturn(false);
        when(builder.buildChunks(10L, 4L)).thenReturn(true);

        BatchEmbeddingResult result = service.buildMissingChunks(10L);

        assertEquals(4, result.total());
        assertEquals(2, result.succeeded());
        assertEquals(2, result.failed());
        assertEquals(List.of(2L, 3L), result.failedNoteIds());
        verify(builder).buildChunks(10L, 4L);
    }

    // 没有缺失片段的笔记时，不调用片段生成服务。
    @Test
    void shouldSkipBuilderWhenNothingIsMissing() {
        NoteMapper mapper = mock(NoteMapper.class);
        NoteChunkBuildService builder = mock(NoteChunkBuildService.class);
        NoteChunkBatchService service = new NoteChunkBatchService(mapper, builder);
        when(mapper.findMissingChunksByUserId(10L)).thenReturn(List.of());

        BatchEmbeddingResult result = service.buildMissingChunks(10L);

        assertEquals(0, result.total());
        assertEquals(0, result.succeeded());
        assertEquals(0, result.failed());
        assertEquals(List.of(), result.failedNoteIds());
        verifyNoInteractions(builder);
    }
}
