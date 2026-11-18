package com.example.aistudyassistant.dto;

public class NoteSearchResult {
    private Long noteId;
    private String title;
    private String content;
    private double score;

    // 初始化 NoteSearchResult 实例。
    public NoteSearchResult(Long noteId, String title, String content, double score) {
        this.noteId = noteId;
        this.title = title;
        this.content = content;
        this.score = score;
    }

    // 获取笔记编号。
    public Long getNoteId() {
        return noteId;
    }

    // 获取标题。
    public String getTitle() {
        return title;
    }

    // 获取内容。
    public String getContent() {
        return content;
    }

    // 获取相似度分数。
    public double getScore() {
        return score;
    }

}
