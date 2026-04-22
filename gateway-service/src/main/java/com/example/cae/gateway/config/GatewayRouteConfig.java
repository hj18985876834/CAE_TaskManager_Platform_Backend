package com.example.cae.gateway.config;

import com.example.cae.gateway.properties.GatewayRouteProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {
    private final GatewayRouteProperties gatewayRouteProperties;

    public GatewayRouteConfig(GatewayRouteProperties gatewayRouteProperties) {
        this.gatewayRouteProperties = gatewayRouteProperties;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r.path("/api/auth/**", "/api/users/**")
                        .uri(gatewayRouteProperties.getUserServiceUri()))
                .route("solver-service", r -> r.path("/api/solvers/**", "/api/profiles/**", "/api/file-rules/**")
                        .uri(gatewayRouteProperties.getSolverServiceUri()))
                .route("scheduler-service", r -> r.path("/api/node-agent/**", "/api/scheduler/nodes/**", "/api/scheduler/records/**", "/api/nodes/**", "/api/schedules/**")
                        .uri(gatewayRouteProperties.getSchedulerServiceUri()))
                .route("task-service", r -> r.path("/api/tasks/**", "/api/admin/tasks/**", "/api/admin/dashboard/**")
                        .uri(gatewayRouteProperties.getTaskServiceUri()))
                .build();
    }
}
