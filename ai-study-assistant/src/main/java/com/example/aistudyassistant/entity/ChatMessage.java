package com.example.aistudyassistant.entity;

import java.time.LocalDateTime;

public class ChatMessage {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;

    // 读取编号字段。
    public Long getId() {
        return id;
    }

    // 设置编号字段。
    public void setId(Long id) {
        this.id = id;
    }

    // 读取会话编号字段。
    public Long getSessionId() {
        return sessionId;
    }

    // 设置会话编号字段。
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    // 读取消息角色字段。
    public String getRole() {
        return role;
    }

    // 设置消息角色字段。
    public void setRole(String role) {
        this.role = role;
    }

    // 读取正文字段。
    public String getContent() {
        return content;
    }

    // 设置正文字段。
    public void setContent(String content) {
        this.content = content;
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
