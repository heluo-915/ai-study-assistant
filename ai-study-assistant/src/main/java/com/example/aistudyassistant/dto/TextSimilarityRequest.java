package com.example.aistudyassistant.dto;

public class TextSimilarityRequest {
    private String textA;
    private String textB;

    // 读取第一段文本字段。
    public String getTextA() {
        return textA;
    }

    // 设置第一段文本字段。
    public void setTextA(String textA) {
        this.textA = textA;
    }

    // 读取第二段文本字段。
    public String getTextB() {
        return textB;
    }

    // 设置第二段文本字段。
    public void setTextB(String textB) {
        this.textB = textB;
    }
}