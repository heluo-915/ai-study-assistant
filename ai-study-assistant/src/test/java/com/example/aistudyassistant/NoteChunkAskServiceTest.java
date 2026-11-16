package com.example.aistudyassistant;

import com.example.aistudyassistant.dto.ChunkAnswerResponse;
import com.example.aistudyassistant.dto.ChunkSearchResult;
import com.example.aistudyassistant.dto.ChunkSource;
import com.example.aistudyassistant.service.ChatService;
import com.example.aistudyassistant.service.NoteChunkAskService;
import com.example.aistudyassistant.service.NoteChunkSearchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NoteChunkAskServiceTest {

    // 未找到片段时直接返回空来源，不调用对话模型。
    @Test
    void shouldNotCallModelWhenNoChunksMatch() {
        NoteChunkSearchService search = mock(NoteChunkSearchService.class);
        ChatService chat = mock(ChatService.class);
        NoteChunkAskService service = new NoteChunkAskService(search, chat);
        when(search.search(10L, "问题")).thenReturn(List.of());

        ChunkAnswerResponse result = service.ask(10L, "问题");

        assertEquals("没有找到相关笔记片段", result.answer());
        assertEquals(List.of(), result.sources());
        verifyNoInteractions(chat);
    }

    // 多个命中片段应交给模型，来源对应各自笔记和片段序号。
    @Test
    void shouldSendMatchedChunksAndReturnSources() {
        NoteChunkSearchService search = mock(NoteChunkSearchService.class);
        ChatService chat = mock(ChatService.class);
        NoteChunkAskService service = new NoteChunkAskService(search, chat);
        when(search.search(10L, "问题")).thenReturn(List.of(
                new ChunkSearchResult(1L, "笔记一", 0, "第一段正文", 0.9),
                new ChunkSearchResult(2L, "笔记二", 3, "第二段正文", 0.8)
        ));
        when(chat.askAboutNote(anyString(), eq("问题"))).thenReturn("模型回答");

        ChunkAnswerResponse result = service.ask(10L, "问题");

        ArgumentCaptor<String> reference = ArgumentCaptor.forClass(String.class);
        verify(chat).askAboutNote(reference.capture(), eq("问题"));
        assertTrue(reference.getValue().contains("笔记一"));
        assertTrue(reference.getValue().contains("第一段正文"));
        assertTrue(reference.getValue().contains("笔记二"));
        assertTrue(reference.getValue().contains("第二段正文"));
        assertEquals("模型回答", result.answer());
        assertEquals(List.of(new ChunkSource(1L, "笔记一", 0), new ChunkSource(2L, "笔记二", 3)), result.sources());
    }
}
