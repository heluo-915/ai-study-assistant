package com.example.aistudyassistant.exception;

import com.example.aistudyassistant.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 将模型调用异常转换为统一的 HTTP 响应。
    @ExceptionHandler(ModelServiceException.class)
    public ResponseEntity<ApiError> handleModelServiceException(
            ModelServiceException exception
    ) {
        String causeType = exception.getCause() == null
                ? "未知"
                : exception.getCause().getClass().getSimpleName();

        log.warn("模型调用失败：{}，原因类型：{}", exception.getMessage(), causeType);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("MODEL_SERVICE_ERROR", exception.getMessage()));
    }
}