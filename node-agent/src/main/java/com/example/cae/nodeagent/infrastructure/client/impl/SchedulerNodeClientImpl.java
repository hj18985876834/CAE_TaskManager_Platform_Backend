package com.example.cae.nodeagent.infrastructure.client.impl;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.response.Result;
import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.domain.model.NodeInfo;
import com.example.cae.nodeagent.infrastructure.client.SchedulerNodeClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class SchedulerNodeClientImpl implements SchedulerNodeClient {
	private final RestTemplate restTemplate;
	private final NodeAgentConfig nodeAgentConfig;

	public SchedulerNodeClientImpl(RestTemplate restTemplate, NodeAgentConfig nodeAgentConfig) {
		this.restTemplate = restTemplate;
		this.nodeAgentConfig = nodeAgentConfig;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void register(NodeInfo nodeInfo) {
		String url = nodeAgentConfig.getSchedulerBaseUrl() + "/api/node-agent/register";
		Map<String, Object> body = new HashMap<>();
		body.put("nodeCode", nodeInfo.getNodeCode());
		body.put("nodeName", nodeInfo.getNodeName());
		body.put("host", nodeInfo.getHost());
		body.put("maxConcurrency", nodeInfo.getMaxConcurrency());
		body.put("solvers", toSolverItems(nodeInfo.getSolverIds()));
		Result<Object> result = restTemplate.postForObject(url, body, Result.class);
		if (result == null || !(result.getData() instanceof Map<?, ?> dataMap)) {
			return;
		}
		Object nodeId = dataMap.get("nodeId");
		Object nodeToken = dataMap.get("nodeToken");
		if (nodeId != null) {
			try {
				nodeAgentConfig.setNodeId(Long.parseLong(String.valueOf(nodeId)));
			} catch (NumberFormatException ignored) {
				// keep configured id if parsing fails
			}
		}
		if (nodeToken != null) {
			nodeAgentConfig.setNodeToken(String.valueOf(nodeToken));
		}
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
		restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Object.class);
	}

	private java.util.List<Map<String, Object>> toSolverItems(java.util.List<Long> solverIds) {
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

	private String resolveSolverVersion(Long solverId) {
		if (solverId == null || nodeAgentConfig.getSolverVersions() == null) {
			return "v1";
		}
		String version = nodeAgentConfig.getSolverVersions().get(String.valueOf(solverId));
		if (version == null || version.isBlank()) {
			return "v1";
		}
		return version;
	}
}