package com.example.cae.scheduler.infrastructure.client.impl;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.config.SchedulerRemoteServiceProperties;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class NodeAgentClientStub implements NodeAgentClient {
	private final RestTemplate restTemplate;
	private final ComputeNodeRepository computeNodeRepository;
	private final SchedulerRemoteServiceProperties remoteServiceProperties;

	public NodeAgentClientStub(RestTemplate restTemplate,
							   ComputeNodeRepository computeNodeRepository,
							   SchedulerRemoteServiceProperties remoteServiceProperties) {
		this.restTemplate = restTemplate;
		this.computeNodeRepository = computeNodeRepository;
		this.remoteServiceProperties = remoteServiceProperties;
	}

	@Override
	public void notifyDispatch(Long nodeId, TaskDTO task) {
		ComputeNode node = computeNodeRepository.findById(nodeId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found: " + nodeId));
		String baseUrl = buildNodeAgentBaseUrl(node);
		String url = baseUrl + "/internal/dispatch-task";

		Map<String, Object> request = new HashMap<>();
		request.put("taskId", task.getTaskId());
		request.put("taskNo", task.getTaskNo());
		request.put("solverId", task.getSolverId());
		request.put("solverCode", task.getSolverCode());
		request.put("solverExecMode", task.getSolverExecMode());
		request.put("solverExecPath", task.getSolverExecPath());
		request.put("profileId", task.getProfileId());
		request.put("taskType", task.getTaskType());
		request.put("commandTemplate", task.getCommandTemplate());
		request.put("parserName", task.getParserName());
		request.put("timeoutSeconds", task.getTimeoutSeconds() == null ? 600 : task.getTimeoutSeconds());
		request.put("inputFiles", task.getInputFiles() == null ? java.util.List.of() : task.getInputFiles());
		request.put("params", task.getParams() == null ? java.util.Map.of() : task.getParams());

		Result<NodeAgentActionAck> result = restTemplate.exchange(
				url,
				HttpMethod.POST,
				withNodeToken(request, node),
				new ParameterizedTypeReference<Result<NodeAgentActionAck>>() {
				}
		).getBody();
		validateDispatchResult(result);
		NodeAgentActionAck ack = result == null ? null : result.getData();
		if (ack == null || ack.getAccepted() == null) {
			throw new BizException(ErrorCodeConstants.NODE_AGENT_EMPTY_RESPONSE, "node-agent dispatch response is empty");
		}
		if (!Boolean.TRUE.equals(ack.getAccepted())) {
			String message = ack.getMessage();
			throw new BizException(ErrorCodeConstants.NODE_AGENT_REJECTED, message == null ? "node-agent rejected task" : message);
		}
	}

	private void validateDispatchResult(Result<?> result) {
		if (result == null) {
			throw new BizException(ErrorCodeConstants.NODE_AGENT_EMPTY_RESPONSE, "node-agent dispatch response is empty");
		}
		Integer code = result.getCode();
		if (code == null || code == 0) {
			return;
		}
		String message = result.getMessage() == null || result.getMessage().isBlank()
				? "node-agent dispatch failed"
				: result.getMessage();
		if (code == ErrorCodeConstants.CONFLICT) {
			throw new BizException(ErrorCodeConstants.NODE_AGENT_REJECTED, message);
		}
		throw new BizException(code, message);
	}

	@Override
	public void cancelTask(Long nodeId, Long taskId, String reason) {
		ComputeNode node = computeNodeRepository.findById(nodeId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found: " + nodeId));
		String baseUrl = buildNodeAgentBaseUrl(node);
		String url = baseUrl + "/internal/cancel-task";

		Map<String, Object> request = new HashMap<>();
		request.put("taskId", taskId);
		request.put("reason", reason);

		Result<NodeAgentActionAck> result = restTemplate.exchange(
				url,
				HttpMethod.POST,
				withNodeToken(request, node),
				new ParameterizedTypeReference<Result<NodeAgentActionAck>>() {
				}
		).getBody();
		validateResult(result, "cancel task");
		NodeAgentActionAck ack = result == null ? null : result.getData();
		if (ack == null || ack.getAccepted() == null) {
			throw new BizException(ErrorCodeConstants.NODE_AGENT_EMPTY_RESPONSE, "node-agent cancel response is empty");
		}
		if (!Boolean.TRUE.equals(ack.getAccepted())) {
			String message = ack.getMessage();
			throw new BizException(ErrorCodeConstants.NODE_AGENT_REJECTED, message == null ? "node-agent rejected cancel" : message);
		}
	}

	@Override
	public boolean isTaskActive(Long nodeId, Long taskId) {
		ComputeNode node = computeNodeRepository.findById(nodeId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found: " + nodeId));
		String baseUrl = buildNodeAgentBaseUrl(node);
		String url = baseUrl + "/internal/tasks/" + taskId + "/runtime";
		Result<NodeAgentRuntimeStatus> result = restTemplate.exchange(
				url,
				HttpMethod.GET,
				withNodeToken(null, node),
				new ParameterizedTypeReference<Result<NodeAgentRuntimeStatus>>() {
				}
		).getBody();
		validateResult(result, "runtime status");
		NodeAgentRuntimeStatus status = result == null ? null : result.getData();
		if (status == null || status.getTaskId() == null || status.getActive() == null) {
			throw new BizException(ErrorCodeConstants.NODE_AGENT_EMPTY_RESPONSE, "node-agent runtime status response is empty");
		}
		if (!status.getTaskId().equals(taskId)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "node-agent runtime status response taskId mismatch");
		}
		return Boolean.TRUE.equals(status.getActive());
	}

	private String buildNodeAgentBaseUrl(ComputeNode node) {
		if (node == null || node.getHost() == null || node.getHost().isBlank()) {
			throw new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node host is empty");
		}
		String host = node.getHost().trim();
		Integer port = node.getPort();
		String scheme = remoteServiceProperties.getNodeAgentScheme();
		if (scheme == null || scheme.isBlank()) {
			scheme = "http";
		}
		if (host.startsWith("http://") || host.startsWith("https://")) {
			URI uri = URI.create(host);
			if (uri.getPort() > 0 || port == null || port <= 0) {
				return stripTrailingSlash(host);
			}
			return stripTrailingSlash(host) + ":" + port;
		}
		if (hasExplicitPort(host)) {
			return scheme + "://" + stripTrailingSlash(host);
		}
		if (port == null || port <= 0) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "node port is empty");
		}
		return scheme + "://" + stripTrailingSlash(host) + ":" + port;
	}

	private boolean hasExplicitPort(String host) {
		return host != null && host.matches("^[^/]+:\\d+$");
	}

	private String stripTrailingSlash(String value) {
		if (value == null) {
			return null;
		}
		int end = value.length();
		while (end > 0 && value.charAt(end - 1) == '/') {
			end--;
		}
		return value.substring(0, end);
	}

	private void validateResult(Result<?> result, String action) {
		if (result == null) {
			throw new BizException(ErrorCodeConstants.NODE_AGENT_EMPTY_RESPONSE, "node-agent " + action + " response is empty");
		}
		if (result.getCode() != null && result.getCode() != 0) {
			throw new BizException(result.getCode(), result.getMessage() == null ? "node-agent " + action + " failed" : result.getMessage());
		}
	}

	private HttpEntity<?> withNodeToken(Object body, ComputeNode node) {
		if (node == null || node.getNodeToken() == null || node.getNodeToken().isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "node token is empty");
		}
		HttpHeaders headers = new HttpHeaders();
		headers.set(HeaderConstants.X_NODE_TOKEN, node.getNodeToken());
		return new HttpEntity<>(body, headers);
	}

	private static class NodeAgentActionAck {
		private Boolean accepted;
		private String message;

		public Boolean getAccepted() {
			return accepted;
		}

		public void setAccepted(Boolean accepted) {
			this.accepted = accepted;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	private static class NodeAgentRuntimeStatus {
		private Long taskId;
		private Boolean active;

		public Long getTaskId() {
			return taskId;
		}

		public void setTaskId(Long taskId) {
			this.taskId = taskId;
		}

		public Boolean getActive() {
			return active;
		}

		public void setActive(Boolean active) {
			this.active = active;
		}
	}
}
