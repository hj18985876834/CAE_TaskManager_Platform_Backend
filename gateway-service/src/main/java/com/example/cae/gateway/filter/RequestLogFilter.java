package com.example.cae.gateway.filter;

import com.example.cae.common.constant.HeaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestLogFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() == null ? "UNKNOWN" : exchange.getRequest().getMethod().name();
        String traceId = exchange.getRequest().getHeaders().getFirst(HeaderConstants.TRACE_ID);
        return chain.filter(exchange).doFinally(signalType -> {
            long cost = System.currentTimeMillis() - start;
            log.info(buildAccessLog(traceId, method, path, cost));
        });
    }

    @Override
    public int getOrder() {
        return -200;
    }

    public String buildAccessLog(String traceId, String method, String path, long costMs) {
        return "gateway request, traceId=" + traceId + ", method=" + method + ", path=" + path + ", cost=" + costMs + "ms";
    }
}
