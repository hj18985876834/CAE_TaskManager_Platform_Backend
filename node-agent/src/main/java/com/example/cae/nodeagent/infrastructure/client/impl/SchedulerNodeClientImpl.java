package com.example.cae.nodeagent.infrastructure.client.impl;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.dto.TaskStatusAckDTO;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.Result;
import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.domain.model.NodeInfo;
import com.example.cae.nodeagent.infrastructure.client.SchedulerNodeClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SchedulerNodeClientImpl implements SchedulerNodeClient {
	private final RestTemplate restTemplate;
	private final NodeAgentConfig nodeAgentConfig;

	public SchedulerNodeClientImpl(RestTemplate restTemplate, NodeAgentConfig nodeAgentConfig) {
		this.restTemplate = restTemplate;
		this.nodeAgentConfig = nodeAgentConfig;
	}

	@Override
	public void register(NodeInfo nodeInfo) {
		String url = nodeAgentConfig.getSchedulerBaseUrl() + "/api/node-agent/register";
		List<Long> solverIds = normalizeSolverIds(nodeInfo == null ? null : nodeInfo.getSolverIds());
		Map<String, Object> body = new HashMap<>();
		body.put("nodeCode", nodeInfo.getNodeCode());
		body.put("nodeName", nodeInfo.getNodeName());
		body.put("host", nodeInfo.getHost());
		body.put("port", nodeInfo.getPort());
		body.put("maxConcurrency", nodeInfo.getMaxConcurrency());
		body.put("solvers", toSolverItems(solverIds));
		Result<NodeRegisterAck> result = restTemplate.exchange(
				url,
				HttpMethod.POST,
				new HttpEntity<>(body),
				new ParameterizedTypeReference<Result<NodeRegisterAck>>() {
				}
		).getBody();
		validateResult(result, "register node");
		NodeRegisterAck ack = result.getData();
		if (ack == null || ack.getNodeId() == null || ack.getNodeToken() == null || ack.getNodeToken().isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "register node response data is invalid");
		}
		nodeAgentConfig.setNodeId(ack.getNodeId());
		nodeAgentConfig.setNodeToken(ack.getNodeToken());
	}

	@Override
	public void heartbeat(NodeInfo nodeInfo) {
		String url = nodeAgentConfig.getSchedulerBaseUrl() + "/api/node-agent/heartbeat";
		Map<String, Object> body = new HashMap<>();
		body.put("nodeId", nodeAgentConfig.getNodeId());
		body.put("nodeCode", nodeInfo.getNodeCode());
		body.put("cpuUsage", nodeInfo.getCpuUsage());
		body.put("memoryUsage", nodeInfo.getMemoryUsage());
		body.put("runningCount", nodeInfo.getRunningCount());
		HttpHeaders headers = new HttpHeaders();
		headers.set(HeaderConstants.X_NODE_TOKEN, nodeAgentConfig.getNodeToken());
		Result<?> result = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Result.class);
		validateResult(result, "heartbeat");
	}

	@Override
	public void dispatchFailed(Long taskId, String failType, String reason, boolean recoverable) {
		if (taskId == null) {
			return;
		}
		String url = nodeAgentConfig.getSchedulerBaseUrl()
				+ "/internal/tasks/" + taskId + "/dispatch-failed";
		Map<String, Object> body = new HashMap<>();
		body.put("nodeId", nodeAgentConfig.getNodeId());
		body.put("failType", failType);
		body.put("reason", reason);
		body.put("recoverable", recoverable);
		HttpHeaders headers = new HttpHeaders();
		headers.set(HeaderConstants.X_NODE_TOKEN, nodeAgentConfig.getNodeToken());
		Result<TaskStatusAckDTO> result = restTemplate.exchange(
				url,
				HttpMethod.POST,
				new HttpEntity<>(body, headers),
				new ParameterizedTypeReference<Result<TaskStatusAckDTO>>() {
				}
		).getBody();
		validateResult(result, "dispatch failed");
		TaskStatusAckDTO ack = result.getData();
		if (ack == null || ack.getTaskId() == null || ack.getStatus() == null || ack.getStatus().isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "dispatch failed response data is invalid");
		}
		if (!ack.getTaskId().equals(taskId)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "dispatch failed response taskId mismatch");
		}
	}

	private void validateResult(Result<?> result, String action) {
		if (result == null) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response is empty");
		}
		if (result.getCode() != null && result.getCode() != 0) {
			throw new BizException(result.getCode(), result.getMessage(), result.getData());
		}
	}

	private List<Map<String, Object>> toSolverItems(List<Long> solverIds) {
		if (solverIds == null || solverIds.isEmpty()) {
			return java.util.List.of();
		}
		return solverIds.stream().filter(id -> id != null).map(id -> {
			Map<String, Object> item = new HashMap<>();
			item.put("solverId", id);
			item.put("solverVersion", resolveSolverVersion(id));
			return item;
		}).toList();
	}

	private List<Long> normalizeSolverIds(List<Long> solverIds) {
		if (solverIds == null || solverIds.isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "solverIds cannot be empty");
		}
		Set<Long> uniqueSolverIds = new LinkedHashSet<>();
		for (Long solverId : solverIds) {
			if (solverId == null || solverId < 1) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid solverId in node config");
			}
			if (!uniqueSolverIds.add(solverId)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "duplicate solverId in node config: " + solverId);
			}
		}
		return List.copyOf(uniqueSolverIds);
	}

	private String resolveSolverVersion(Long solverId) {
		if (solverId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "solverId is required");
		}
		if (nodeAgentConfig.getSolverVersions() == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "solverVersions config is missing");
		}
		String version = nodeAgentConfig.getSolverVersions().get(String.valueOf(solverId));
		if (version == null || version.isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST,
					"solver version config missing for solverId=" + solverId);
		}
		return version.trim();
	}

	private static class NodeRegisterAck {
		private Long nodeId;
		private String nodeToken;

		public Long getNodeId() {
			return nodeId;
		}

		public void setNodeId(Long nodeId) {
			this.nodeId = nodeId;
		}

		public String getNodeToken() {
			return nodeToken;
		}

		public void setNodeToken(String nodeToken) {
			this.nodeToken = nodeToken;
		}
	}
}
