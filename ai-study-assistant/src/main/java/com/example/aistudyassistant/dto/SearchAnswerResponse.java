package com.example.aistudyassistant.dto;

import java.util.List;

public class SearchAnswerResponse {
    private String answer;
    private List<NoteSource> sources;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public SearchAnswerResponse(String answer, List<NoteSource> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    // 读取回答字段。
    public String getAnswer() {
        return answer;
    }

    // 设置回答字段。
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    // 读取来源列表字段。
    public List<NoteSource> getSources() {
        return sources;
    }

    // 设置来源列表字段。
    public void setSources(List<NoteSource> sources) {
        this.sources = sources;
    }
}
