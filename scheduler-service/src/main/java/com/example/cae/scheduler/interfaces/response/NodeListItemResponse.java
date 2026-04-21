package com.example.cae.scheduler.interfaces.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NodeListItemResponse {
	private Long id;
	private Long nodeId;
	private String nodeCode;
	private String nodeName;
	private String host;
	private String status;
	private Integer enabled;
	private Integer runningCount;
	private Integer reservedCount;
	private Integer maxConcurrency;
	private Integer effectiveLoad;
	private java.math.BigDecimal loadRatio;
	private BigDecimal cpuUsage;
	private BigDecimal memoryUsage;
	private LocalDateTime lastHeartbeatTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
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

	public java.math.BigDecimal getLoadRatio() {
		return loadRatio;
	}

	public void setLoadRatio(java.math.BigDecimal loadRatio) {
		this.loadRatio = loadRatio;
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

	public LocalDateTime getLastHeartbeatTime() {
		return lastHeartbeatTime;
	}

	public void setLastHeartbeatTime(LocalDateTime lastHeartbeatTime) {
		this.lastHeartbeatTime = lastHeartbeatTime;
	}
}
