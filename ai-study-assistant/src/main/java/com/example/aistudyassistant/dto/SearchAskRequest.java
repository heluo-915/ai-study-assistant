package com.example.aistudyassistant.dto;

public class SearchAskRequest {
    private String keyword;
    private String question;

    // 读取关键词字段。
    public String getKeyword() {
        return keyword;
    }

    // 设置关键词字段。
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    // 读取问题字段。
    public String getQuestion() {
        return question;
    }

    // 设置问题字段。
    public void setQuestion(String question) {
        this.question = question;
    }
}
