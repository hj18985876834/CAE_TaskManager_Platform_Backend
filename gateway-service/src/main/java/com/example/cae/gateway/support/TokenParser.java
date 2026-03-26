package com.example.cae.gateway.support;

public class TokenParser {
    public String resolveBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}
