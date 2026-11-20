package com.blog.interfaces.rest.support;

import java.time.LocalDateTime;

/**
 * 统一响应包装。code 与 HTTP 状态保持一致，便于前端统一处理。
 */
public record ApiResponse<T>(int code, String message, T data, LocalDateTime timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "OK", data, LocalDateTime.now());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(200, "OK", null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now());
    }
}
