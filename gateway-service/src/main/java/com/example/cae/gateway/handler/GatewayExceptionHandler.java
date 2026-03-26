package com.example.cae.gateway.handler;

import com.example.cae.common.response.Result;

public class GatewayExceptionHandler {
    public Result<Void> handle(Throwable ex) {
        return Result.fail(500, ex.getMessage());
    }
}
