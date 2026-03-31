package com.example.cae.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r.path("/api/auth/**", "/api/users/**")
                        .uri("http://localhost:8081"))
                .route("solver-service", r -> r.path("/api/solvers/**", "/api/profiles/**", "/api/file-rules/**")
                        .uri("http://localhost:8082"))
                .route("scheduler-service", r -> r.path("/api/tasks/*/schedules", "/api/node-agent/**", "/api/scheduler/nodes/**", "/api/scheduler/records/**", "/api/nodes/**", "/api/schedules/**")
                        .uri("http://localhost:8084"))
                .route("task-service", r -> r.path("/api/tasks/**", "/api/admin/tasks/**", "/api/admin/dashboard/**")
                        .uri("http://localhost:8083"))
                .build();
    }
}
