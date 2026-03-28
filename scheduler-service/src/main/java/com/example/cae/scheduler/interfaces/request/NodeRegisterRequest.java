package com.example.cae.scheduler.interfaces.request;

import java.util.List;

public class NodeRegisterRequest {
	private String nodeCode;
	private String nodeName;
	private String host;
	private Integer maxConcurrency;
	private List<Long> solverIds;

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

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(Integer maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public List<Long> getSolverIds() {
		return solverIds;
	}

	public void setSolverIds(List<Long> solverIds) {
		this.solverIds = solverIds;
	}
}

