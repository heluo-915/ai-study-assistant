package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.SearchAnswerResponse;
import com.example.aistudyassistant.entity.Note;
import org.springframework.stereotype.Service;
import com.example.aistudyassistant.dto.NoteSource;
import java.util.ArrayList;

import java.util.List;

@Service
public class NoteSearchAskService {
    private final NoteService noteService;
    private  final  ChatService chatService;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public NoteSearchAskService(NoteService noteService, ChatService chatService) {
        this.noteService = noteService;
        this.chatService = chatService;
    }

    // 检索用户笔记并组装参考内容、回答和来源。
    public SearchAnswerResponse ask(Long userId, String keyword, String question){
        List<Note> noteList=noteService.searchByKeyword(userId, keyword);
        if (noteList.isEmpty()) {
            return new SearchAnswerResponse("没有找到相关笔记", List.of());
        }
        StringBuilder reference = new StringBuilder();
        List<NoteSource> sources = new ArrayList<>();

        for (Note note : noteList) {
            reference.append("笔记标题：")
                    .append(note.getTitle())
                    .append("\n笔记内容：")
                    .append(note.getContent())
                    .append("\n\n");

            sources.add(new NoteSource(note.getId(), note.getTitle()));
        }
        String answer = chatService.askAboutNote(reference.toString(), question);
        return new SearchAnswerResponse(answer, sources);
    }

}
