package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.ChunkAnswerResponse;
import com.example.aistudyassistant.dto.ChunkSearchResult;
import com.example.aistudyassistant.dto.ChunkSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NoteChunkAskService {

    private final NoteChunkSearchService noteChunkSearchService;
    private final ChatService chatService;

    public NoteChunkAskService(
            NoteChunkSearchService noteChunkSearchService,
            ChatService chatService
    ) {
        this.noteChunkSearchService = noteChunkSearchService;
        this.chatService = chatService;
    }

    // 根据当前用户的相关笔记片段回答问题。
    public ChunkAnswerResponse ask(Long userId, String question) {
        List<ChunkSearchResult> results =
                noteChunkSearchService.search(userId, question);

        if (results.isEmpty()) {
            return new ChunkAnswerResponse("没有找到相关笔记片段", List.of());
        }

        StringBuilder reference = new StringBuilder();
        List<ChunkSource> sources = new ArrayList<>();

        for (ChunkSearchResult result : results) {
            reference.append("笔记标题：")
                    .append(result.noteTitle())
                    .append("\n片段序号：")
                    .append(result.chunkIndex())
                    .append("\n片段内容：")
                    .append(result.content())
                    .append("\n\n");

            sources.add(new ChunkSource(
                    result.noteId(),
                    result.noteTitle(),
                    result.chunkIndex()
            ));
        }

        String answer = chatService.askAboutNote(reference.toString(), question);
        return new ChunkAnswerResponse(answer, sources);
    }
}