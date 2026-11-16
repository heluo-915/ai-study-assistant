package com.example.aistudyassistant.dto;

public class NoteAnswerResponse {
    private String answer;
    private Long noteId;
    private String noteTitle;
    // 通过构造方法接收并保存所需依赖或初始化字段。
    public NoteAnswerResponse(String answer, Long noteId, String noteTitle) {
        this.answer = answer;
        this.noteId = noteId;
        this.noteTitle = noteTitle;
    }
    // 读取回答字段。
    public String getAnswer() {
        return answer;
    }

    // 读取笔记编号字段。
    public Long getNoteId() {
        return noteId;
    }

    // 读取noteTitle字段。
    public String getNoteTitle() {
        return noteTitle;
    }
}
