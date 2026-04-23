package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class ResultSummaryReportRequest {
	@NotNull(message = "nodeId不能为空")
	@Positive(message = "nodeId必须大于0")
	private Long nodeId;
	@NotNull(message = "successFlag不能为空")
	@Min(value = 0, message = "successFlag只能是0或1")
	@Max(value = 1, message = "successFlag只能是0或1")
	private Integer successFlag;
	@Min(value = 0, message = "durationSeconds不能小于0")
	private Integer durationSeconds;
	@Size(max = 500, message = "summaryText长度不能超过500")
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
