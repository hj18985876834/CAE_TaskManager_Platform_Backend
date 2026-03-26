package com.example.cae.nodeagent.domain.model;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ExecutionResult {
	private Boolean success;
	private Integer durationSeconds;
	private String summaryText;
	private Map<String, Object> metrics;
	private List<File> resultFiles;

	public static ExecutionResult success(Integer durationSeconds, String summaryText, Map<String, Object> metrics, List<File> resultFiles) {
		ExecutionResult result = new ExecutionResult();
		result.setSuccess(true);
		result.setDurationSeconds(durationSeconds);
		result.setSummaryText(summaryText);
		result.setMetrics(metrics);
		result.setResultFiles(resultFiles);
		return result;
	}

	public static ExecutionResult fail(Integer durationSeconds, String summaryText) {
		ExecutionResult result = new ExecutionResult();
		result.setSuccess(false);
		result.setDurationSeconds(durationSeconds);
		result.setSummaryText(summaryText);
		return result;
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
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

	public List<File> getResultFiles() {
		return resultFiles;
	}

	public void setResultFiles(List<File> resultFiles) {
		this.resultFiles = resultFiles;
	}
}

