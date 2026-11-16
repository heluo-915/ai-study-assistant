package com.example.aistudyassistant.entity;

import java.time.LocalDateTime;

public class Note {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private LocalDateTime createdAt;

    // 读取编号字段。
    public Long getId() {
        return id;
    }

    // 设置编号字段。
    public void setId(Long id) {
        this.id = id;
    }

    // 读取标题字段。
    public String getTitle() {
        return title;
    }

    // 设置标题字段。
    public void setTitle(String title) {
        this.title = title;
    }

    // 读取正文字段。
    public String getContent() {
        return content;
    }

    // 设置正文字段。
    public void setContent(String content) {
        this.content = content;
    }

    // 读取用户编号字段。
    public Long getUserId() {
        return userId;
    }

    // 设置用户编号字段。
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // 读取创建时间字段。
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // 设置创建时间字段。
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
