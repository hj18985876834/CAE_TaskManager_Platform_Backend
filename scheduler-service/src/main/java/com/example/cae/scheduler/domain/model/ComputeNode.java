package com.example.cae.scheduler.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ComputeNode {
	private Long id;
	private String nodeCode;
	private String nodeName;
	private String host;
	private String nodeToken;
	private String status;
	private Integer enabled;
	private Integer maxConcurrency;
	private Integer runningCount;
	private Integer reservedCount;
	private BigDecimal cpuUsage;
	private BigDecimal memoryUsage;
	private LocalDateTime lastHeartbeatTime;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public void markOnline() {
		this.status = "ONLINE";
	}

	public void markOffline() {
		this.status = "OFFLINE";
	}

	public void refreshHeartbeat(BigDecimal cpuUsage, BigDecimal memoryUsage, Integer runningCount, LocalDateTime heartbeatTime) {
		this.cpuUsage = cpuUsage;
		this.memoryUsage = memoryUsage;
		this.runningCount = runningCount;
		this.lastHeartbeatTime = heartbeatTime;
	}

	public boolean isOnline() {
		return "ONLINE".equals(this.status);
	}

	public boolean isEnabled() {
		return this.enabled != null && this.enabled == 1;
	}

	public void enable() {
		this.enabled = 1;
	}

	public void disable() {
		this.enabled = 0;
	}

	public boolean canDispatch() {
		return isOnline()
				&& isEnabled()
				&& this.maxConcurrency != null
				&& getTotalLoad() < this.maxConcurrency;
	}

	public boolean reserveSlot() {
		if (!canDispatch()) {
			return false;
		}
		this.reservedCount = currentReservedCount() + 1;
		return true;
	}

	public void releaseReservation() {
		this.reservedCount = Math.max(0, currentReservedCount() - 1);
	}

	public int getTotalLoad() {
		return currentRunningCount() + currentReservedCount();
	}

	private int currentRunningCount() {
		return this.runningCount == null ? 0 : this.runningCount;
	}

	private int currentReservedCount() {
		return this.reservedCount == null ? 0 : this.reservedCount;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getNodeToken() {
		return nodeToken;
	}

	public void setNodeToken(String nodeToken) {
		this.nodeToken = nodeToken;
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

	public Integer getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(Integer maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
