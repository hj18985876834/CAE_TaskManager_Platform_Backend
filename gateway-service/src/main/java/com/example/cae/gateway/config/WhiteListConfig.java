package com.example.cae.gateway.config;

import com.example.cae.gateway.properties.GatewaySecurityProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class WhiteListConfig {
    private static final List<String> DEFAULT_WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/logout",
            "/actuator/health"
    );

    private final GatewaySecurityProperties gatewaySecurityProperties;

    public WhiteListConfig(GatewaySecurityProperties gatewaySecurityProperties) {
        this.gatewaySecurityProperties = gatewaySecurityProperties;
    }

    public List<String> getWhiteListPaths() {
        List<String> configured = gatewaySecurityProperties.getWhiteList();
        return configured == null || configured.isEmpty() ? DEFAULT_WHITE_LIST : configured;
    }
}
