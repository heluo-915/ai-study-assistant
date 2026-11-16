package com.example.aistudyassistant.dto;

// 返回给客户端的简短错误信息。
public record ApiError(
        String code,
        String message
) {
}