package com.example.cae.nodeagent.infrastructure.client.impl;

import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import com.example.cae.nodeagent.infrastructure.client.TaskReportClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Component
public class TaskReportClientImpl implements TaskReportClient {
	private final RestTemplate restTemplate;
	private final NodeAgentConfig nodeAgentConfig;

	public TaskReportClientImpl(RestTemplate restTemplate, NodeAgentConfig nodeAgentConfig) {
		this.restTemplate = restTemplate;
		this.nodeAgentConfig = nodeAgentConfig;
	}

	@Override
	public void markDispatched(Long taskId, Long nodeId) {
		String url = UriComponentsBuilder
				.fromHttpUrl(taskBaseUrl() + "/internal/tasks/{taskId}/mark-dispatched")
				.queryParam("nodeId", nodeId)
				.buildAndExpand(taskId)
				.toUriString();
		restTemplate.postForEntity(url, null, Object.class);
	}

	@Override
	public void reportStatus(Long taskId, String status, String reason) {
		String url = taskBaseUrl() + "/internal/tasks/" + taskId + "/status-report";
		Map<String, Object> body = new HashMap<>();
		body.put("status", status);
		body.put("reason", reason);
		restTemplate.postForEntity(url, body, Object.class);
	}

	@Override
	public void reportLog(Long taskId, Integer seqNo, String content) {
		String url = taskBaseUrl() + "/internal/tasks/" + taskId + "/log-report";
		Map<String, Object> body = new HashMap<>();
		body.put("seqNo", seqNo);
		body.put("content", content);
		restTemplate.postForEntity(url, body, Object.class);
	}

	@Override
	public void reportResultSummary(Long taskId, ExecutionResult result) {
		String url = taskBaseUrl() + "/internal/tasks/" + taskId + "/result-summary-report";
		Map<String, Object> body = new HashMap<>();
		body.put("success", result.getSuccess());
		body.put("durationSeconds", result.getDurationSeconds());
		body.put("summaryText", result.getSummaryText());
		body.put("metrics", result.getMetrics());
		restTemplate.postForEntity(url, body, Object.class);
	}

	@Override
	public void reportResultFile(Long taskId, File file) {
		String url = taskBaseUrl() + "/internal/tasks/" + taskId + "/result-file-report";
		Map<String, Object> body = new HashMap<>();
		body.put("fileType", getFileType(file));
		body.put("fileName", file == null ? null : file.getName());
		body.put("storagePath", file == null ? null : file.getAbsolutePath());
		body.put("fileSize", file == null ? null : file.length());
		restTemplate.postForEntity(url, body, Object.class);
	}

	@Override
	public void markFinished(Long taskId) {
		String url = taskBaseUrl() + "/internal/tasks/" + taskId + "/mark-finished";
		restTemplate.postForEntity(url, null, Object.class);
	}

	@Override
	public void markFailed(Long taskId, String failType, String failMessage) {
		String url = UriComponentsBuilder
				.fromHttpUrl(taskBaseUrl() + "/internal/tasks/{taskId}/mark-failed")
				.queryParam("failType", failType)
				.queryParam("failMessage", failMessage)
				.buildAndExpand(taskId)
				.toUriString();
		restTemplate.postForEntity(url, null, Object.class);
	}

	private String taskBaseUrl() {
		return nodeAgentConfig.getTaskBaseUrl();
	}

	private String getFileType(File file) {
		if (file == null) {
			return "unknown";
		}
		String name = file.getName();
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == name.length() - 1) {
			return "unknown";
		}
		return name.substring(dotIndex + 1).toLowerCase();
	}
}