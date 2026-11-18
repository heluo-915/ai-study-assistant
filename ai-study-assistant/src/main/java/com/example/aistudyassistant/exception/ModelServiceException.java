package com.example.aistudyassistant.exception;

// 表示调用模型服务时发生错误。
public class ModelServiceException extends RuntimeException {

    // 初始化 ModelServiceException 实例。
    public ModelServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}