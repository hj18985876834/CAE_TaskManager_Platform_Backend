package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class NodeHeartbeatRequest {
	@NotNull(message = "nodeId不能为空")
	@Positive(message = "nodeId必须大于0")
	private Long nodeId;
	@NotNull(message = "cpuUsage不能为空")
	@DecimalMin(value = "0.0", message = "cpuUsage不能小于0")
	@DecimalMax(value = "100.0", message = "cpuUsage不能大于100")
	private BigDecimal cpuUsage;
	@NotNull(message = "memoryUsage不能为空")
	@DecimalMin(value = "0.0", message = "memoryUsage不能小于0")
	@DecimalMax(value = "100.0", message = "memoryUsage不能大于100")
	private BigDecimal memoryUsage;
	@NotNull(message = "runningCount不能为空")
	@Min(value = 0, message = "runningCount不能小于0")
	private Integer runningCount;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
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
}
