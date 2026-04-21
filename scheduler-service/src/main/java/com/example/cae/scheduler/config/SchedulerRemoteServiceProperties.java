package com.example.cae.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cae.remote")
public class SchedulerRemoteServiceProperties {
	private String taskBaseUrl = "http://localhost:8083";
	private String solverBaseUrl = "http://localhost:8082";
	private String nodeAgentScheme = "http";

	public String getTaskBaseUrl() {
		return taskBaseUrl;
	}

	public void setTaskBaseUrl(String taskBaseUrl) {
		this.taskBaseUrl = taskBaseUrl;
	}

	public String getSolverBaseUrl() {
		return solverBaseUrl;
	}

	public void setSolverBaseUrl(String solverBaseUrl) {
		this.solverBaseUrl = solverBaseUrl;
	}

	public String getNodeAgentScheme() {
		return nodeAgentScheme;
	}

	public void setNodeAgentScheme(String nodeAgentScheme) {
		this.nodeAgentScheme = nodeAgentScheme;
	}
}
