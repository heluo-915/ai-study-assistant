package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.NoteSearchResult;
import com.example.aistudyassistant.dto.NoteSource;
import com.example.aistudyassistant.dto.SearchAnswerResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class NoteSemanticAskService {
    private final NoteSemanticSearchService noteSemanticSearchService;
    private final ChatService chatService;

    // 初始化 NoteSemanticAskService 实例。
    public NoteSemanticAskService(NoteSemanticSearchService noteSemanticSearchService, ChatService chatService) {
        this.noteSemanticSearchService = noteSemanticSearchService;
        this.chatService = chatService;
    }
    // 根据当前用户的相关笔记回答问题，并返回使用的笔记来源。
    public SearchAnswerResponse ask(Long userId, String question) {
        List<NoteSearchResult> results =
                noteSemanticSearchService.search(userId, question);

        // 没有相关笔记时，不调用 DeepSeek
        if (results.isEmpty()) {
            return new SearchAnswerResponse("没有找到相关笔记", List.of());
        }

        StringBuilder reference = new StringBuilder();
        List<NoteSource> sources = new ArrayList<>();

        for (NoteSearchResult result : results) {
            reference.append("笔记标题：")
                    .append(result.getTitle())
                    .append("\n笔记内容：")
                    .append(result.getContent())
                    .append("\n\n");

            sources.add(new NoteSource(result.getNoteId(), result.getTitle()));
        }

        String answer = chatService.askAboutNote(reference.toString(), question);
        return new SearchAnswerResponse(answer, sources);
    }
}
