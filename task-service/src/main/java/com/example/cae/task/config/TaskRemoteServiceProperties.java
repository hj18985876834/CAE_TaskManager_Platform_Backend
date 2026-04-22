package com.example.cae.task.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cae.remote")
public class TaskRemoteServiceProperties {
	private String solverBaseUrl = "http://localhost:8082";
	private String schedulerBaseUrl = "http://localhost:8084";
	private String userBaseUrl = "http://localhost:8081";

	public String getSolverBaseUrl() {
		return solverBaseUrl;
	}

	public void setSolverBaseUrl(String solverBaseUrl) {
		this.solverBaseUrl = solverBaseUrl;
	}

	public String getSchedulerBaseUrl() {
		return schedulerBaseUrl;
	}

	public void setSchedulerBaseUrl(String schedulerBaseUrl) {
		this.schedulerBaseUrl = schedulerBaseUrl;
	}

	public String getUserBaseUrl() {
		return userBaseUrl;
	}

	public void setUserBaseUrl(String userBaseUrl) {
		this.userBaseUrl = userBaseUrl;
	}
}
