package com.example.cae.gateway.config;

import com.example.cae.gateway.properties.GatewaySecurityProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class WhiteListConfig {
    private static final List<String> DEFAULT_WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/logout",
            "/api/node-agent/register",
            "/api/node-agent/heartbeat",
            "/actuator/health"
    );

    private final GatewaySecurityProperties gatewaySecurityProperties;

    public WhiteListConfig(GatewaySecurityProperties gatewaySecurityProperties) {
        this.gatewaySecurityProperties = gatewaySecurityProperties;
    }

	public List<String> getWhiteListPaths() {
		List<String> configured = gatewaySecurityProperties.getWhiteList();
		if (configured == null || configured.isEmpty()) {
			return DEFAULT_WHITE_LIST;
		}
		Set<String> merged = new LinkedHashSet<>(DEFAULT_WHITE_LIST);
		merged.addAll(configured);
		return List.copyOf(merged);
	}
}
