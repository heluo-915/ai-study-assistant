package com.example.aistudyassistant.entity;

public class NoteEmbedding {
    private Long noteId;
    private String title;
    private String content;
    private String embedding;
    private String modelName;

    // 获取笔记编号。
    public Long getNoteId() {
        return noteId;
    }

    // 设置笔记编号。
    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    // 获取标题。
    public String getTitle() {
        return title;
    }

    // 设置标题。
    public void setTitle(String title) {
        this.title = title;
    }

    // 获取内容。
    public String getContent() {
        return content;
    }

    // 设置内容。
    public void setContent(String content) {
        this.content = content;
    }

    // 获取向量数据。
    public String getEmbedding() {
        return embedding;
    }

    // 设置向量数据。
    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    // 获取向量模型名称。
    public String getModelName() {
        return modelName;
    }

    // 设置向量模型名称。
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
