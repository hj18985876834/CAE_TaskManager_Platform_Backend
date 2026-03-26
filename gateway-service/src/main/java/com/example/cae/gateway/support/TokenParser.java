package com.example.cae.gateway.support;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.constant.SecurityConstants;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class TokenParser {
    public String resolveToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HeaderConstants.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (!authorization.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return null;
        }
        return authorization.substring(SecurityConstants.TOKEN_PREFIX.length());
    }
}
