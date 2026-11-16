package com.example.aistudyassistant.dto;

public class QuizOption {
    private String label;   // 选项标记，如 A、B
    private String content; // 选项内容

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
