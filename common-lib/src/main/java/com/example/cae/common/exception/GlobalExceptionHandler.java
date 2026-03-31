package com.example.cae.common.exception;

import com.example.cae.common.response.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Object> handleBizException(BizException ex) {
        return Result.fail(ex.getCode(), ex.getMessage(), ex.getData());
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

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException ex) {
        return Result.fail(400, ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null || error.getDefaultMessage().isBlank()
                        ? error.getField() + " is invalid"
                        : error.getDefaultMessage())
                .orElse("request binding failed"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("request parameter validation failed");
        return Result.fail(400, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return Result.fail(400, (ex.getName() == null ? "parameter" : ex.getName()) + " type mismatch");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String message = ex.getMostSpecificCause() == null ? "request body is invalid" : ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains(":")) {
            message = message.substring(0, message.indexOf(':'));
        }
        return Result.fail(400, message == null || message.isBlank() ? "request body is invalid" : message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        return Result.fail(500, ex.getMessage());
    }
}
