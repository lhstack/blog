package com.blog.interfaces.rest.support;

/**
 * 接口层业务异常：把领域错误码映射为 HTTP 状态。
 */
public class BizException extends RuntimeException {

    private final int status;

    public BizException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
