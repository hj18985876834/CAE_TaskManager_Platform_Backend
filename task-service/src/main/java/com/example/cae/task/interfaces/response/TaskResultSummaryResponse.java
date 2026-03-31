package com.example.cae.task.interfaces.response;

import java.time.LocalDateTime;
import java.util.Map;

public class TaskResultSummaryResponse {
	private Long id;
	private Long taskId;
	private Integer successFlag;
	private Integer durationSeconds;
	private String summaryText;
	private Map<String, Object> metrics;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
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
