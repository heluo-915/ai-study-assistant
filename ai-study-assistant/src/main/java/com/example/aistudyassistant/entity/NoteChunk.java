package com.example.aistudyassistant.entity;

public class NoteChunk {
    private Long noteId;
    private String noteTitle;
    private int chunkIndex;
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

    // 获取笔记标题。
    public String getNoteTitle() {
        return noteTitle;
    }

    // 设置笔记标题。
    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    // 获取片段序号。
    public int getChunkIndex() {
        return chunkIndex;
    }

    // 设置片段序号。
    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
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
