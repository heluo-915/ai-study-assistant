package com.example.aistudyassistant.dto;

public class HelloResponse {

    private String name;
    private String message;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public HelloResponse(String name, String message) {
        this.name = name;
        this.message = message;
    }

    // 读取名称字段。
    public String getName() {
        return name;
    }

    // 读取消息内容字段。
    public String getMessage() {
        return message;
    }
}