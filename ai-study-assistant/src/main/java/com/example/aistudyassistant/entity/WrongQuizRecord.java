package com.example.aistudyassistant.entity;

// 保存错题查询额外返回的最近作答结果。
public class WrongQuizRecord extends QuizRecord {
    private boolean latestCorrect;
    public boolean isLatestCorrect() { return latestCorrect; }
    public void setLatestCorrect(boolean latestCorrect) { this.latestCorrect = latestCorrect; }
}
