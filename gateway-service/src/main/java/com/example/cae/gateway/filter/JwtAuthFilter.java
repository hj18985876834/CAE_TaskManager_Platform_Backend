package com.example.cae.gateway.filter;

import com.example.cae.common.enums.RoleCodeEnum;
import com.example.cae.common.exception.ForbiddenException;
import com.example.cae.common.exception.UnauthorizedException;
import com.example.cae.common.utils.JwtUtil;
import com.example.cae.gateway.config.WhiteListConfig;
import com.example.cae.gateway.properties.GatewaySecurityProperties;
import com.example.cae.gateway.support.GatewayRequestMutator;
import com.example.cae.gateway.support.PathMatcherSupport;
import com.example.cae.gateway.support.TokenParser;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {
    private static final List<String> DEFAULT_ADMIN_ONLY_PATHS = List.of(
            "/api/admin/**",
            "/api/users/**",
            "/api/nodes/**",
            "/api/schedules/**"
    );
    private static final List<String> DEFAULT_ADMIN_WRITE_PATHS = List.of(
            "/api/solvers/**",
            "/api/profiles/**",
            "/api/file-rules/**"
    );

    private final WhiteListConfig whiteListConfig;
    private final GatewaySecurityProperties gatewaySecurityProperties;
    private final PathMatcherSupport pathMatcherSupport;
    private final TokenParser tokenParser;
    private final GatewayRequestMutator gatewayRequestMutator;

    public JwtAuthFilter(WhiteListConfig whiteListConfig,
                         GatewaySecurityProperties gatewaySecurityProperties,
                         PathMatcherSupport pathMatcherSupport,
                         TokenParser tokenParser,
                         GatewayRequestMutator gatewayRequestMutator) {
        this.whiteListConfig = whiteListConfig;
        this.gatewaySecurityProperties = gatewaySecurityProperties;
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
        if (isAdminOnlyRequest(exchange.getRequest()) && !RoleCodeEnum.ADMIN.name().equalsIgnoreCase(roleCode)) {
            return Mono.error(new ForbiddenException("admin role required"));
        }
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

    private boolean isAdminOnlyRequest(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        if (pathMatcherSupport.matchAny(path, mergePaths(DEFAULT_ADMIN_ONLY_PATHS, gatewaySecurityProperties.getAdminOnlyPaths()))) {
            return true;
        }
        if (method == null) {
            return false;
        }
        boolean writeMethod = HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.DELETE.equals(method);
        if (!writeMethod) {
            return false;
        }
        return pathMatcherSupport.matchAny(path, mergePaths(DEFAULT_ADMIN_WRITE_PATHS, gatewaySecurityProperties.getAdminWritePaths()));
    }

    private List<String> mergePaths(List<String> defaults, List<String> configured) {
        if (configured == null || configured.isEmpty()) {
            return defaults;
        }
        Set<String> merged = new LinkedHashSet<>(defaults);
        merged.addAll(configured);
        return List.copyOf(merged);
    }
}
