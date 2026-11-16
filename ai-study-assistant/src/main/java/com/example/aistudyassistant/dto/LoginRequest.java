package com.example.aistudyassistant.dto;

public class LoginRequest {
    private String username;
    private String password;

    // 读取用户名字段。
    public String getUsername() {
        return username;
    }

    // 设置用户名字段。
    public void setUsername(String username) {
        this.username = username;
    }

    // 读取密码字段。
    public String getPassword() {
        return password;
    }

    // 设置密码字段。
    public void setPassword(String password) {
        this.password = password;
    }
}
