package com.shixiaoyu.xiangyueproject.exception;

import lombok.Data;

@Data
public class CommonException extends RuntimeException {
    private int code;

    public CommonException(int code, String message) {
        super(message);
        this.code = code;
    }

    public CommonException(int code, Throwable cause, String message) {
        super(message, cause);
        this.code = 500;
    }

    public CommonException(int code, Throwable cause) {
        super(cause);
        this.code = 500;
    }
}
