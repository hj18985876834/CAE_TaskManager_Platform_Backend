package com.example.cae.gateway.filter;

import com.example.cae.common.exception.UnauthorizedException;
import com.example.cae.common.utils.JwtUtil;
import com.example.cae.gateway.config.WhiteListConfig;
import com.example.cae.gateway.support.GatewayRequestMutator;
import com.example.cae.gateway.support.PathMatcherSupport;
import com.example.cae.gateway.support.TokenParser;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {
    private final WhiteListConfig whiteListConfig;
    private final PathMatcherSupport pathMatcherSupport;
    private final TokenParser tokenParser;
    private final GatewayRequestMutator gatewayRequestMutator;

    public JwtAuthFilter(WhiteListConfig whiteListConfig,
                         PathMatcherSupport pathMatcherSupport,
                         TokenParser tokenParser,
                         GatewayRequestMutator gatewayRequestMutator) {
        this.whiteListConfig = whiteListConfig;
        this.pathMatcherSupport = pathMatcherSupport;
        this.tokenParser = tokenParser;
        this.gatewayRequestMutator = gatewayRequestMutator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }
        String token = tokenParser.resolveToken(exchange.getRequest());
        if (token == null || !validate(token)) {
            return Mono.error(new UnauthorizedException("token invalid or missing"));
        }
        Long userId = JwtUtil.parseUserId(token);
        String roleCode = JwtUtil.parseRoleCode(token);
        ServerHttpRequest mutatedRequest = writeUserHeaders(exchange.getRequest(), userId, roleCode);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    public boolean isWhitePath(String path) {
        return pathMatcherSupport.matchAny(path, whiteListConfig.getWhiteListPaths());
    }

    public boolean validate(String token) {
        return JwtUtil.validateToken(token);
    }

    public ServerHttpRequest writeUserHeaders(ServerHttpRequest request, Long userId, String roleCode) {
        return gatewayRequestMutator.withUserContext(request, userId, roleCode);
    }
}
