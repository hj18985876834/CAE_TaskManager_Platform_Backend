package com.example.cae.nodeagent.infrastructure.client.impl;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import com.example.cae.nodeagent.infrastructure.client.TaskReportClient;
import com.example.cae.nodeagent.infrastructure.storage.PathMappingSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
	private final PathMappingSupport pathMappingSupport;

	public TaskReportClientImpl(RestTemplate restTemplate, NodeAgentConfig nodeAgentConfig, PathMappingSupport pathMappingSupport) {
		this.restTemplate = restTemplate;
		this.nodeAgentConfig = nodeAgentConfig;
		this.pathMappingSupport = pathMappingSupport;
	}

	@Override
	public void reportStatus(Long taskId, String status, String reason) {
		String url = taskBaseUrl() + "/internal/tasks/" + taskId + "/status-report";
		Map<String, Object> body = new HashMap<>();
		body.put("nodeId", nodeAgentConfig.getNodeId());
		body.put("fromStatus", null);
		body.put("toStatus", status);
		body.put("changeReason", reason);
		body.put("operatorType", "NODE");
		restTemplate.postForEntity(url, withToken(body), Object.class);
	}

	@Override
	public void reportLog(Long taskId, Integer seqNo, String content) {
		String url = taskBaseUrl() + "/internal/tasks/" + taskId + "/log-report";
		Map<String, Object> body = new HashMap<>();
		body.put("nodeId", nodeAgentConfig.getNodeId());
		body.put("seqNo", seqNo);
		body.put("logContent", content);
		restTemplate.postForEntity(url, withToken(body), Object.class);
	}

	@Override
	public void reportResultSummary(Long taskId, ExecutionResult result) {
		String url = taskBaseUrl() + "/internal/tasks/" + taskId + "/result-summary-report";
		Map<String, Object> body = new HashMap<>();
		body.put("nodeId", nodeAgentConfig.getNodeId());
		body.put("successFlag", Boolean.TRUE.equals(result.getSuccess()) ? 1 : 0);
		body.put("durationSeconds", result.getDurationSeconds());
		body.put("summaryText", result.getSummaryText());
		body.put("metrics", result.getMetrics());
		restTemplate.postForEntity(url, withToken(body), Object.class);
	}

	@Override
	public void reportResultFile(Long taskId, File file) {
		String url = taskBaseUrl() + "/internal/tasks/" + taskId + "/result-file-report";
		Map<String, Object> body = new HashMap<>();
		body.put("nodeId", nodeAgentConfig.getNodeId());
		body.put("fileType", getFileType(file));
		body.put("fileName", file == null ? null : file.getName());
		body.put("storagePath", file == null ? null : pathMappingSupport.toWindowsPath(file.getAbsolutePath()));
		body.put("fileSize", file == null ? null : file.length());
		restTemplate.postForEntity(url, withToken(body), Object.class);
	}

	@Override
	public void markFinished(Long taskId) {
		String url = taskBaseUrl() + "/internal/tasks/" + taskId + "/mark-finished";
		Map<String, Object> body = new HashMap<>();
		body.put("nodeId", nodeAgentConfig.getNodeId());
		body.put("finalStatus", "SUCCESS");
		restTemplate.postForEntity(url, withToken(body), Object.class);
	}

	@Override
	public void markFailed(Long taskId, String failType, String failMessage) {
		String url = UriComponentsBuilder
				.fromHttpUrl(taskBaseUrl() + "/internal/tasks/{taskId}/mark-failed")
				.buildAndExpand(taskId)
				.toUriString();
		Map<String, Object> body = new HashMap<>();
		body.put("nodeId", nodeAgentConfig.getNodeId());
		body.put("failType", failType);
		body.put("failMessage", failMessage);
		restTemplate.postForEntity(url, withToken(body), Object.class);
	}

	private String taskBaseUrl() {
		return nodeAgentConfig.getTaskBaseUrl();
	}

	private String getFileType(File file) {
		if (file == null) {
			return "RESULT";
		}
		String name = file.getName().toLowerCase();
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == name.length() - 1) {
			return "RESULT";
		}
		String suffix = name.substring(dotIndex + 1);
		if ("log".equals(suffix) || "txt".equals(suffix)) {
			return "LOG";
		}
		if ("html".equals(suffix) || "pdf".equals(suffix) || "doc".equals(suffix) || "docx".equals(suffix)) {
			return "REPORT";
		}
		if ("png".equals(suffix) || "jpg".equals(suffix) || "jpeg".equals(suffix) || "bmp".equals(suffix)) {
			return "IMAGE";
		}
		return "RESULT";
	}

	private HttpEntity<?> withToken(Object body) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HeaderConstants.X_NODE_TOKEN, nodeAgentConfig.getNodeToken());
		return new HttpEntity<>(body, headers);
	}
}
