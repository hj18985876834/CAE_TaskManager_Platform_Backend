package com.example.cae.nodeagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "cae.node")
public class NodeAgentConfig {
	private Long nodeId = 1L;
	private String nodeCode = "NODE_1";
	private String nodeName = "Worker_Node_1";
	private Integer nodePort = 8085;
	private Integer maxConcurrency = 2;
	private String schedulerBaseUrl = "http://localhost:8084";
	private String taskBaseUrl = "http://localhost:8083";
	private String workRoot = "./node-agent-work";
	private List<Long> solverIds;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getNodeCode() {
		return nodeCode;
	}

	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public Integer getNodePort() {
		return nodePort;
	}

	public void setNodePort(Integer nodePort) {
		this.nodePort = nodePort;
	}

	public Integer getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(Integer maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public String getSchedulerBaseUrl() {
		return schedulerBaseUrl;
	}

	public void setSchedulerBaseUrl(String schedulerBaseUrl) {
		this.schedulerBaseUrl = schedulerBaseUrl;
	}

	public String getTaskBaseUrl() {
		return taskBaseUrl;
	}

	public void setTaskBaseUrl(String taskBaseUrl) {
		this.taskBaseUrl = taskBaseUrl;
	}

	public String getWorkRoot() {
		return workRoot;
	}

	public void setWorkRoot(String workRoot) {
		this.workRoot = workRoot;
	}

	public List<Long> getSolverIds() {
		return solverIds;
	}

	public void setSolverIds(List<Long> solverIds) {
		this.solverIds = solverIds;
	}
}