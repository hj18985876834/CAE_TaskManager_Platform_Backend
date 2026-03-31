package com.example.cae.task.interfaces.response;

import java.math.BigDecimal;

public class TaskDashboardSummaryResponse {
	private Long totalTaskCount;
	private Long runningTaskCount;
	private Long queuedTaskCount;
	private BigDecimal successRate;
	private Integer onlineNodeCount;
	private BigDecimal avgNodeLoad;

	public Long getTotalTaskCount() {
		return totalTaskCount;
	}

	public void setTotalTaskCount(Long totalTaskCount) {
		this.totalTaskCount = totalTaskCount;
	}

	public Long getRunningTaskCount() {
		return runningTaskCount;
	}

	public void setRunningTaskCount(Long runningTaskCount) {
		this.runningTaskCount = runningTaskCount;
	}

	public Long getQueuedTaskCount() {
		return queuedTaskCount;
	}

	public void setQueuedTaskCount(Long queuedTaskCount) {
		this.queuedTaskCount = queuedTaskCount;
	}

	public BigDecimal getSuccessRate() {
		return successRate;
	}

	public void setSuccessRate(BigDecimal successRate) {
		this.successRate = successRate;
	}

	public Integer getOnlineNodeCount() {
		return onlineNodeCount;
	}

	public void setOnlineNodeCount(Integer onlineNodeCount) {
		this.onlineNodeCount = onlineNodeCount;
	}

	public BigDecimal getAvgNodeLoad() {
		return avgNodeLoad;
	}

	public void setAvgNodeLoad(BigDecimal avgNodeLoad) {
		this.avgNodeLoad = avgNodeLoad;
	}
}
