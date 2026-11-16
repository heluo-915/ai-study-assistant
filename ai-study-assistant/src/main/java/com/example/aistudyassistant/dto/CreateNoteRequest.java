package com.example.aistudyassistant.dto;

public class CreateNoteRequest {
    private String title;
    private String content;

    // 读取标题字段。
    public String getTitle() {
        return title;
    }

    // 设置标题字段。
    public void setTitle(String title) {
        this.title = title;
    }

    // 读取正文字段。
    public String getContent() {
        return content;
    }

    // 设置正文字段。
    public void setContent(String content) {
        this.content = content;
    }
}
