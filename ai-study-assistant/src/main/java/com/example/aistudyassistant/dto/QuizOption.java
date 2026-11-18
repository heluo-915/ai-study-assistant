package com.example.aistudyassistant.dto;

public class QuizOption {
    private String label;   // 选项标记，如 A、B
    private String content; // 选项内容

    // 获取Label 字段。
    public String getLabel() {
        return label;
    }

    // 设置Label 字段。
    public void setLabel(String label) {
        this.label = label;
    }

    // 获取内容。
    public String getContent() {
        return content;
    }

    // 设置内容。
    public void setContent(String content) {
        this.content = content;
    }
}
