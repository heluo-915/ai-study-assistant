package com.example.aistudyassistant.dto;

public class NoteSource {
    private Long noteId;
    private String title;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public NoteSource(Long noteId, String title) {
        this.noteId = noteId;
        this.title = title;
    }

    // 读取笔记编号字段。
    public Long getNoteId() {
        return noteId;
    }

    // 设置笔记编号字段。
    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    // 读取标题字段。
    public String getTitle() {
        return title;
    }

    // 设置标题字段。
    public void setTitle(String title) {
        this.title = title;
    }
}
