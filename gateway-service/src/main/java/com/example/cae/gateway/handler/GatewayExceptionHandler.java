package com.example.cae.gateway.handler;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.ForbiddenException;
import com.example.cae.common.exception.UnauthorizedException;
import com.example.cae.common.response.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;

    public GatewayExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<Void> result;
        HttpStatus status;
        if (ex instanceof UnauthorizedException) {
            result = Result.fail(ErrorCodeConstants.UNAUTHORIZED, ex.getMessage());
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof ForbiddenException) {
            result = Result.fail(ErrorCodeConstants.FORBIDDEN, ex.getMessage());
            status = HttpStatus.FORBIDDEN;
        } else {
            result = Result.fail(ErrorCodeConstants.INTERNAL_SERVER_ERROR, ex.getMessage());
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        exchange.getResponse().setStatusCode(status);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException jsonProcessingException) {
            bytes = ("{\"code\":" + ErrorCodeConstants.INTERNAL_SERVER_ERROR + ",\"message\":\"gateway error\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
