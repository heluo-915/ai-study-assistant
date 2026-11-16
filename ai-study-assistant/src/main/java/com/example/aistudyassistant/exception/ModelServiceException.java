package com.example.aistudyassistant.exception;

// 表示调用模型服务时发生错误。
public class ModelServiceException extends RuntimeException {

    public ModelServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}