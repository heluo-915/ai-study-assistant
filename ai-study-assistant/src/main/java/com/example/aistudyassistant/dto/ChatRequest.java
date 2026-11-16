package com.example.aistudyassistant.dto;

public class ChatRequest {
    private String message;

    // 读取消息内容字段。
    public String getMessage() {
        return message;
    }

    // 设置消息内容字段。
    public void setMessage(String message) {
        this.message = message;
    }
}
