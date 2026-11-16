package com.example.aistudyassistant.entity;

public class User {
    private Long id;
    private String username;
    private String passwordHash;

    // 读取编号字段。
    public Long getId() {
        return id;
    }

    // 设置编号字段。
    public void setId(Long id) {
        this.id = id;
    }

    // 读取用户名字段。
    public String getUsername() {
        return username;
    }

    // 设置用户名字段。
    public void setUsername(String username) {
        this.username = username;
    }

    // 读取密码哈希字段。
    public String getPasswordHash() {
        return passwordHash;
    }

    // 设置密码哈希字段。
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
