package com.example.cae.gateway.config;

import java.util.List;

public class WhiteListConfig {
    public List<String> getWhiteListPaths() {
        return List.of("/api/auth/login", "/api/auth/logout", "/actuator/health");
    }
}
