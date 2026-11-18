package com.example.aistudyassistant.entity;

public class QuizRecord {
    private Long id;
    private Long noteId;
    private String question;
    private String options;
    private String correctAnswer;
    private String explanation;
    // 获取编号。
    public Long getId() { return id; }
    // 设置编号。
    public void setId(Long id) { this.id = id; }
    // 获取笔记编号。
    public Long getNoteId() { return noteId; }
    // 设置笔记编号。
    public void setNoteId(Long noteId) { this.noteId = noteId; }
    // 获取问题。
    public String getQuestion() { return question; }
    // 设置问题。
    public void setQuestion(String question) { this.question = question; }
    // 获取Options 字段。
    public String getOptions() { return options; }
    // 设置Options 字段。
    public void setOptions(String options) { this.options = options; }
    // 获取CorrectAnswer 字段。
    public String getCorrectAnswer() { return correctAnswer; }
    // 设置CorrectAnswer 字段。
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    // 获取Explanation 字段。
    public String getExplanation() { return explanation; }
    // 设置Explanation 字段。
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
