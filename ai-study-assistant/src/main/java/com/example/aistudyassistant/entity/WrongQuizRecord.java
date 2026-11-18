package com.example.aistudyassistant.entity;

// 保存错题查询额外返回的最近作答结果。
public class WrongQuizRecord extends QuizRecord {
    private boolean latestCorrect;
    // 获取最近一次作答是否正确。
    public boolean isLatestCorrect() { return latestCorrect; }
    // 设置LatestCorrect 字段。
    public void setLatestCorrect(boolean latestCorrect) { this.latestCorrect = latestCorrect; }
}
