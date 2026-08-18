package com.shixiaoyu.xiangyueproject.exception;

import lombok.Getter;

/**
 * 业务异常：业务层校验失败直接抛出，由 GlobalExceptionHandler 统一转成 Result
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
