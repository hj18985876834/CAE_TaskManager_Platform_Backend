package com.example.cae.scheduler.infrastructure.client.impl;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class NodeAgentClientStub implements NodeAgentClient {
	private final RestTemplate restTemplate;
	private final ComputeNodeRepository computeNodeRepository;

	public NodeAgentClientStub(RestTemplate restTemplate, ComputeNodeRepository computeNodeRepository) {
		this.restTemplate = restTemplate;
		this.computeNodeRepository = computeNodeRepository;
	}

	@Override
	public void notifyDispatch(Long nodeId, TaskDTO task) {
		ComputeNode node = computeNodeRepository.findById(nodeId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found: " + nodeId));
		String baseUrl = node.getHost() != null && node.getHost().startsWith("http")
				? node.getHost()
				: "http://" + node.getHost();
		String url = baseUrl + "/internal/dispatch-task";

		Map<String, Object> request = new HashMap<>();
		request.put("taskId", task.getTaskId());
		request.put("taskNo", task.getTaskNo());
		request.put("solverId", task.getSolverId());
		request.put("solverCode", task.getSolverCode() == null || task.getSolverCode().isBlank() ? "MOCK" : task.getSolverCode());
		request.put("profileId", task.getProfileId());
		request.put("taskType", task.getTaskType());
		request.put("commandTemplate", task.getCommandTemplate());
		request.put("parserName", task.getParserName());
		request.put("timeoutSeconds", task.getTimeoutSeconds() == null ? 600 : task.getTimeoutSeconds());
		request.put("inputFiles", task.getInputFiles() == null ? java.util.List.of() : task.getInputFiles());
		request.put("params", task.getParams() == null ? java.util.Map.of() : task.getParams());

		Result<?> result = restTemplate.postForObject(url, request, Result.class);
		if (result == null || !(result.getData() instanceof Map<?, ?> dataMap)) {
			throw new BizException(ErrorCodeConstants.NODE_AGENT_EMPTY_RESPONSE, "node-agent dispatch response is empty");
		}
		Object accepted = dataMap.get("accepted");
		if (!(accepted instanceof Boolean acceptedFlag) || !acceptedFlag) {
			Object message = dataMap.get("message");
			throw new BizException(ErrorCodeConstants.NODE_AGENT_REJECTED, message == null ? "node-agent rejected task" : String.valueOf(message));
		}
	}

	@Override
	public void cancelTask(Long nodeId, Long taskId, String reason) {
		ComputeNode node = computeNodeRepository.findById(nodeId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found: " + nodeId));
		String baseUrl = node.getHost() != null && node.getHost().startsWith("http")
				? node.getHost()
				: "http://" + node.getHost();
		String url = baseUrl + "/internal/cancel-task";

		Map<String, Object> request = new HashMap<>();
		request.put("taskId", taskId);
		request.put("reason", reason);

		Result<?> result = restTemplate.postForObject(url, request, Result.class);
		if (result == null || !(result.getData() instanceof Map<?, ?> dataMap)) {
			throw new BizException(ErrorCodeConstants.NODE_AGENT_EMPTY_RESPONSE, "node-agent cancel response is empty");
		}
		Object accepted = dataMap.get("accepted");
		if (!(accepted instanceof Boolean acceptedFlag) || !acceptedFlag) {
			Object message = dataMap.get("message");
			throw new BizException(ErrorCodeConstants.NODE_AGENT_REJECTED, message == null ? "node-agent rejected cancel" : String.valueOf(message));
		}
	}
}
