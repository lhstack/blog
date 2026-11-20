package com.blog.domain.common;

/**
 * 领域异常：领域层唯一允许抛出的异常类型，携带稳定错误码，便于接口层映射 HTTP 状态。
 */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static DomainException notFound(String message) {
        return new DomainException("NOT_FOUND", message);
    }

    public static DomainException invalid(String message) {
        return new DomainException("INVALID_ARGUMENT", message);
    }

    public static DomainException conflict(String message) {
        return new DomainException("CONFLICT", message);
    }

    public static DomainException unauthorized(String message) {
        return new DomainException("UNAUTHORIZED", message);
    }
}
