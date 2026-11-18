package com.example.aistudyassistant.dto;

import java.util.List;

public class QuizQuestion {
    private String question;          // 题目
    private List<QuizOption> options; // 选项列表
    private String correctAnswer;     // 正确选项标记，如 A
    private String explanation;      // 答案解析

    // 获取问题。
    public String getQuestion() {
        return question;
    }

    // 设置问题。
    public void setQuestion(String question) {
        this.question = question;
    }

    // 获取Options 字段。
    public List<QuizOption> getOptions() {
        return options;
    }

    // 设置Options 字段。
    public void setOptions(List<QuizOption> options) {
        this.options = options;
    }

    // 获取CorrectAnswer 字段。
    public String getCorrectAnswer() {
        return correctAnswer;
    }

    // 设置CorrectAnswer 字段。
    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    // 获取Explanation 字段。
    public String getExplanation() {
        return explanation;
    }

    // 设置Explanation 字段。
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
