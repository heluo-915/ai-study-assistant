package com.example.aistudyassistant.dto;

import java.util.List;

public class QuizQuestion {
    private String question;          // 题目
    private List<QuizOption> options; // 选项列表
    private String correctAnswer;     // 正确选项标记，如 A
    private String explanation;      // 答案解析

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<QuizOption> getOptions() {
        return options;
    }

    public void setOptions(List<QuizOption> options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
