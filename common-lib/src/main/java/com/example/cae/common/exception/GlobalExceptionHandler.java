package com.example.cae.common.exception;

import com.example.cae.common.response.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorizedException(UnauthorizedException ex) {
        return Result.fail(401, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public Result<Void> handleForbiddenException(ForbiddenException ex) {
        return Result.fail(403, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public Result<Void> handleNotFoundException(NotFoundException ex) {
        return Result.fail(404, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        return Result.fail(500, ex.getMessage());
    }
}
