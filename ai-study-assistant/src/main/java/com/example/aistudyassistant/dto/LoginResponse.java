package com.example.aistudyassistant.dto;

public class LoginResponse {

    private String token;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public LoginResponse(String token) {
        this.token = token;
    }

    // 读取登录令牌字段。
    public String getToken() {
        return token;
    }
}