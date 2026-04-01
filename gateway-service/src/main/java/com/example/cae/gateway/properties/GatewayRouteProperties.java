package com.example.cae.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway.routes")
public class GatewayRouteProperties {
    private String userServiceUri = "http://localhost:8081";
    private String solverServiceUri = "http://localhost:8082";
    private String schedulerServiceUri = "http://localhost:8084";
    private String taskServiceUri = "http://localhost:8083";

    public String getUserServiceUri() {
        return userServiceUri;
    }

    public void setUserServiceUri(String userServiceUri) {
        this.userServiceUri = userServiceUri;
    }

    public String getSolverServiceUri() {
        return solverServiceUri;
    }

    public void setSolverServiceUri(String solverServiceUri) {
        this.solverServiceUri = solverServiceUri;
    }

    public String getSchedulerServiceUri() {
        return schedulerServiceUri;
    }

    public void setSchedulerServiceUri(String schedulerServiceUri) {
        this.schedulerServiceUri = schedulerServiceUri;
    }

    public String getTaskServiceUri() {
        return taskServiceUri;
    }

    public void setTaskServiceUri(String taskServiceUri) {
        this.taskServiceUri = taskServiceUri;
    }
}
