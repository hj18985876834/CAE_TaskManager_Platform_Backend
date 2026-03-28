package com.example.cae.task.interfaces.request;

import java.util.Map;

public class ResultSummaryReportRequest {
	private Long nodeId;
	private Integer successFlag;
	private Boolean success;
	private Integer durationSeconds;
	private String summaryText;
	private Map<String, Object> metrics;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public Integer getSuccessFlag() {
		return successFlag;
	}

	public void setSuccessFlag(Integer successFlag) {
		this.successFlag = successFlag;
		this.success = successFlag != null && successFlag == 1;
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
		this.successFlag = Boolean.TRUE.equals(success) ? 1 : 0;
	}

	public Integer getDurationSeconds() {
		return durationSeconds;
	}

	public void setDurationSeconds(Integer durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}

	public Map<String, Object> getMetrics() {
		return metrics;
	}

	public void setMetrics(Map<String, Object> metrics) {
		this.metrics = metrics;
	}
}

