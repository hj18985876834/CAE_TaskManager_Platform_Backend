package com.example.cae.scheduler.interfaces.response;

public class AvailableNodeResponse {
	private Long nodeId;
	private String nodeCode;
	private String nodeName;
	private String host;
	private Integer port;
	private Integer runningCount;
	private Integer reservedCount;
	private Integer maxConcurrency;
	private Integer effectiveLoad;

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

	public Integer getRunningCount() {
		return runningCount;
	}

	public void setRunningCount(Integer runningCount) {
		this.runningCount = runningCount;
	}

	public Integer getReservedCount() {
		return reservedCount;
	}

	public void setReservedCount(Integer reservedCount) {
		this.reservedCount = reservedCount;
	}

	public Integer getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(Integer maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public Integer getEffectiveLoad() {
		return effectiveLoad;
	}

	public void setEffectiveLoad(Integer effectiveLoad) {
		this.effectiveLoad = effectiveLoad;
	}
}
