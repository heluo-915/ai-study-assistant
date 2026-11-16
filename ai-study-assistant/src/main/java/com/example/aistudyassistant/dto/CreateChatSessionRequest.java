package com.example.aistudyassistant.dto;

public class CreateChatSessionRequest {
    private String title;

    // 读取标题字段。
    public String getTitle() {
        return title;
    }

    // 设置标题字段。
    public void setTitle(String title) {
        this.title = title;
    }
}
