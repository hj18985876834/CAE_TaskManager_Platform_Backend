package com.example.cae.scheduler.infrastructure.client.impl;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskBasicDTO;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.dto.TaskDispatchAckDTO;
import com.example.cae.common.dto.TaskScheduleClaimDTO;
import com.example.cae.common.dto.TaskStatusAckDTO;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.config.SchedulerRemoteServiceProperties;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TaskClientStub implements TaskClient {
	private final RestTemplate restTemplate;
	private final String taskServiceBaseUrl;

	public TaskClientStub(RestTemplate restTemplate, SchedulerRemoteServiceProperties remoteServiceProperties) {
		this.restTemplate = restTemplate;
		this.taskServiceBaseUrl = remoteServiceProperties.getTaskBaseUrl();
	}

	@Override
	public List<TaskDTO> listPendingTasks(Integer limit) {
		String url = taskServiceBaseUrl + "/internal/tasks/queued";
		if (limit != null && limit > 0) {
			url = url + "?limit=" + limit;
		}
		ResponseEntity<Result<List<TaskDTO>>> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<Result<List<TaskDTO>>>() {
				}
		);
		Result<List<TaskDTO>> body = response.getBody();
		ensureSuccess(body, "list queued tasks");
		if (body == null || body.getData() == null) {
			return List.of();
		}
		return body.getData();
	}

	@Override
	public Map<Long, TaskBasicDTO> getTaskBasics(List<Long> taskIds) {
		if (taskIds == null || taskIds.isEmpty()) {
			return Map.of();
		}
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(taskServiceBaseUrl + "/internal/tasks/basics");
		taskIds.stream()
				.filter(taskId -> taskId != null)
				.distinct()
				.forEach(taskId -> builder.queryParam("taskIds", taskId));
		ResponseEntity<Result<List<TaskBasicDTO>>> response = restTemplate.exchange(
				builder.toUriString(),
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<Result<List<TaskBasicDTO>>>() {
				}
		);
		Result<List<TaskBasicDTO>> body = response.getBody();
		ensureSuccess(body, "get task basics");
		if (body == null || body.getData() == null) {
			return Map.of();
		}
		Map<Long, TaskBasicDTO> taskBasics = new HashMap<>();
		for (TaskBasicDTO item : body.getData()) {
			if (item == null || item.getTaskId() == null) {
				continue;
			}
			taskBasics.put(item.getTaskId(), item);
		}
		return taskBasics;
	}

	@Override
	@SuppressWarnings("unchecked")
	public TaskScheduleClaimDTO markTaskScheduled(Long taskId, Long nodeId) {
		String url = taskServiceBaseUrl + "/internal/tasks/" + taskId + "/mark-scheduled";
		java.util.Map<String, Object> request = new java.util.HashMap<>();
		request.put("nodeId", nodeId);
		Result<Object> result = restTemplate.postForObject(url, request, Result.class);
		ensureSuccess(result, "mark task scheduled");
		if (result == null || result.getData() == null) {
			TaskScheduleClaimDTO response = new TaskScheduleClaimDTO();
			response.setClaimed(Boolean.FALSE);
			response.setTaskId(taskId);
			response.setNodeId(nodeId);
			return response;
		}
		Object data = result.getData();
		if (data instanceof java.util.Map<?, ?> map) {
			TaskScheduleClaimDTO response = new TaskScheduleClaimDTO();
			response.setClaimed(toBoolean(map.get("claimed")));
			response.setTaskId(toLong(map.get("taskId"), taskId));
			response.setNodeId(toLong(map.get("nodeId"), nodeId));
			response.setStatus(map.get("status") == null ? null : String.valueOf(map.get("status")));
			return response;
		}
		TaskScheduleClaimDTO response = new TaskScheduleClaimDTO();
		response.setClaimed(Boolean.parseBoolean(String.valueOf(data)));
		response.setTaskId(taskId);
		response.setNodeId(nodeId);
		return response;
	}

	@Override
	public TaskDispatchAckDTO markTaskDispatched(Long taskId, Long nodeId) {
		String url = taskServiceBaseUrl + "/internal/tasks/" + taskId + "/mark-dispatched";
		java.util.Map<String, Object> request = new java.util.HashMap<>();
		request.put("nodeId", nodeId);
		Result<Object> result = restTemplate.postForObject(url, request, Result.class);
		ensureSuccess(result, "mark task dispatched");
		if (result == null || result.getData() == null) {
			TaskDispatchAckDTO response = new TaskDispatchAckDTO();
			response.setTaskId(taskId);
			response.setNodeId(nodeId);
			return response;
		}
		Object data = result.getData();
		if (data instanceof java.util.Map<?, ?> map) {
			TaskDispatchAckDTO response = new TaskDispatchAckDTO();
			response.setTaskId(toLong(map.get("taskId"), taskId));
			response.setNodeId(toLong(map.get("nodeId"), nodeId));
			response.setStatus(map.get("status") == null ? null : String.valueOf(map.get("status")));
			return response;
		}
		TaskDispatchAckDTO response = new TaskDispatchAckDTO();
		response.setTaskId(taskId);
		response.setNodeId(nodeId);
		return response;
	}

	@Override
	public TaskStatusAckDTO markTaskFailed(Long taskId, Long nodeId, String failType, String reason, boolean recoverable) {
		String url = taskServiceBaseUrl + "/internal/tasks/" + taskId + "/dispatch-failed";
		java.util.Map<String, Object> request = new java.util.HashMap<>();
		request.put("nodeId", nodeId);
		request.put("failType", failType);
		request.put("reason", reason);
		request.put("recoverable", recoverable);
		Result<Object> result = restTemplate.postForObject(url, request, Result.class);
		ensureSuccess(result, "mark task dispatch failed");
		TaskStatusAckDTO response = new TaskStatusAckDTO();
		response.setTaskId(taskId);
		if (result == null || !(result.getData() instanceof Map<?, ?> map)) {
			return response;
		}
		response.setTaskId(toLong(map.get("taskId"), taskId));
		response.setStatus(map.get("status") == null ? null : String.valueOf(map.get("status")));
		return response;
	}

	@Override
	public int markNodeOfflineTasksFailed(Long nodeId, String reason) {
		String url = taskServiceBaseUrl + "/internal/tasks/node-offline/fail";
		java.util.Map<String, Object> request = new java.util.HashMap<>();
		request.put("nodeId", nodeId);
		request.put("reason", reason);
		Result<Object> result = restTemplate.postForObject(url, request, Result.class);
		ensureSuccess(result, "mark node offline tasks failed");
		if (result == null || result.getData() == null) {
			return 0;
		}
		return toInteger(result.getData(), 0);
	}

	private Boolean toBoolean(Object value) {
		if (value == null) {
			return Boolean.FALSE;
		}
		if (value instanceof Boolean bool) {
			return bool;
		}
		return Boolean.parseBoolean(String.valueOf(value));
	}

	private Long toLong(Object value, Long defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(value));
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	private Integer toInteger(Object value, Integer defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	private void ensureSuccess(Result<?> result, String action) {
		if (result == null) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response is empty");
		}
		if (result.getCode() != null && result.getCode() != 0) {
			throw new BizException(result.getCode(), result.getMessage(), result.getData());
		}
	}
}
