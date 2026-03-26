package com.example.cae.gateway.filter;

import com.example.cae.common.constant.HeaderConstants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = generateTraceId();
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(HeaderConstants.TRACE_ID, traceId)
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -300;
    }

    public String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
