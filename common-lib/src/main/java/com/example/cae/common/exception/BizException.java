package com.example.cae.common.exception;

import com.example.cae.common.constant.ErrorCodeConstants;

public class BizException extends RuntimeException {
    private final Integer code;
    private final Object data;

    public BizException(String message) {
        super(message);
        this.code = ErrorCodeConstants.BAD_REQUEST;
        this.data = null;
    }

    public BizException(Integer code, String message) {
        this(code, message, null);
    }

    public BizException(Integer code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }
}
