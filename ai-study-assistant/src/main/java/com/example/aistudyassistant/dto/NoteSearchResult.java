package com.example.aistudyassistant.dto;

public class NoteSearchResult {
    private Long noteId;
    private String title;
    private String content;
    private double score;

    public NoteSearchResult(Long noteId, String title, String content, double score) {
        this.noteId = noteId;
        this.title = title;
        this.content = content;
        this.score = score;
    }

    public Long getNoteId() {
        return noteId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public double getScore() {
        return score;
    }

}
