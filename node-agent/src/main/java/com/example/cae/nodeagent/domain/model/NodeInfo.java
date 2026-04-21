package com.example.cae.nodeagent.domain.model;

import java.math.BigDecimal;
import java.util.List;

public class NodeInfo {
	private String nodeCode;
	private String nodeName;
	private String host;
	private Integer port;
	private Integer maxConcurrency;
	private BigDecimal cpuUsage;
	private BigDecimal memoryUsage;
	private Integer runningCount;
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

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Integer getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(Integer maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public BigDecimal getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(BigDecimal cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	public BigDecimal getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(BigDecimal memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public Integer getRunningCount() {
		return runningCount;
	}

	public void setRunningCount(Integer runningCount) {
		this.runningCount = runningCount;
	}

	public List<Long> getSolverIds() {
		return solverIds;
	}

	public void setSolverIds(List<Long> solverIds) {
		this.solverIds = solverIds;
	}
}
