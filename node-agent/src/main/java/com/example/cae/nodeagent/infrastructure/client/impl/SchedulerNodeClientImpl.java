package com.example.cae.nodeagent.infrastructure.client.impl;

import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.domain.model.NodeInfo;
import com.example.cae.nodeagent.infrastructure.client.SchedulerNodeClient;
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
	public void register(NodeInfo nodeInfo) {
		String url = nodeAgentConfig.getSchedulerBaseUrl() + "/internal/scheduler/nodes/register";
		Map<String, Object> body = new HashMap<>();
		body.put("nodeCode", nodeInfo.getNodeCode());
		body.put("nodeName", nodeInfo.getNodeName());
		body.put("host", nodeInfo.getHost());
		body.put("port", nodeInfo.getPort());
		body.put("maxConcurrency", nodeInfo.getMaxConcurrency());
		body.put("solverIds", nodeInfo.getSolverIds());
		restTemplate.postForEntity(url, body, Object.class);
	}

	@Override
	public void heartbeat(NodeInfo nodeInfo) {
		String url = nodeAgentConfig.getSchedulerBaseUrl() + "/internal/scheduler/nodes/heartbeat";
		Map<String, Object> body = new HashMap<>();
		body.put("nodeCode", nodeInfo.getNodeCode());
		body.put("cpuUsage", nodeInfo.getCpuUsage());
		body.put("memoryUsage", nodeInfo.getMemoryUsage());
		body.put("runningCount", nodeInfo.getRunningCount());
		restTemplate.postForEntity(url, body, Object.class);
	}
}