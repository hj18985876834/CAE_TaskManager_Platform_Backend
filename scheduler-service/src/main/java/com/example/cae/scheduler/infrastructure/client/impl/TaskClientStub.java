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
import org.springframework.http.HttpEntity;
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
		List<TaskDTO> data = requireData(body, "list queued tasks");
		for (TaskDTO item : data) {
			if (item == null || item.getTaskId() == null) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "list queued tasks response contains invalid task item");
			}
		}
		return data;
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
		List<TaskBasicDTO> data = requireData(body, "get task basics");
		Map<Long, TaskBasicDTO> taskBasics = new HashMap<>();
		for (TaskBasicDTO item : data) {
			if (item == null || item.getTaskId() == null || item.getTaskNo() == null || item.getTaskNo().isBlank()) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "get task basics response contains invalid task basic item");
			}
			taskBasics.put(item.getTaskId(), item);
		}
		return taskBasics;
	}

	@Override
	public TaskScheduleClaimDTO markTaskScheduled(Long taskId, Long nodeId) {
		String url = taskServiceBaseUrl + "/internal/tasks/" + taskId + "/mark-scheduled";
		java.util.Map<String, Object> request = new java.util.HashMap<>();
		request.put("nodeId", nodeId);
		ResponseEntity<Result<TaskScheduleClaimDTO>> httpResponse = restTemplate.exchange(
				url,
				HttpMethod.POST,
				new HttpEntity<>(request),
				new ParameterizedTypeReference<Result<TaskScheduleClaimDTO>>() {
				}
		);
		Result<TaskScheduleClaimDTO> result = httpResponse.getBody();
		ensureSuccess(result, "mark task scheduled");
		TaskScheduleClaimDTO responseData = requireData(result, "mark task scheduled");
		if (responseData.getClaimed() == null
				|| responseData.getTaskId() == null
				|| responseData.getNodeId() == null
				|| responseData.getStatus() == null
				|| responseData.getStatus().isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "mark task scheduled response data is invalid");
		}
		return responseData;
	}

	@Override
	public TaskDispatchAckDTO markTaskDispatched(Long taskId, Long nodeId) {
		String url = taskServiceBaseUrl + "/internal/tasks/" + taskId + "/mark-dispatched";
		java.util.Map<String, Object> request = new java.util.HashMap<>();
		request.put("nodeId", nodeId);
		ResponseEntity<Result<TaskDispatchAckDTO>> httpResponse = restTemplate.exchange(
				url,
				HttpMethod.POST,
				new HttpEntity<>(request),
				new ParameterizedTypeReference<Result<TaskDispatchAckDTO>>() {
				}
		);
		Result<TaskDispatchAckDTO> result = httpResponse.getBody();
		ensureSuccess(result, "mark task dispatched");
		TaskDispatchAckDTO responseData = requireData(result, "mark task dispatched");
		if (responseData.getTaskId() == null
				|| responseData.getNodeId() == null
				|| responseData.getStatus() == null
				|| responseData.getStatus().isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "mark task dispatched response data is invalid");
		}
		return responseData;
	}

	@Override
	public TaskStatusAckDTO markTaskFailed(Long taskId, Long nodeId, String failType, String reason, boolean recoverable) {
		String url = taskServiceBaseUrl + "/internal/tasks/" + taskId + "/dispatch-failed";
		java.util.Map<String, Object> request = new java.util.HashMap<>();
		request.put("nodeId", nodeId);
		request.put("failType", failType);
		request.put("reason", reason);
		request.put("recoverable", recoverable);
		ResponseEntity<Result<TaskStatusAckDTO>> response = restTemplate.exchange(
				url,
				HttpMethod.POST,
				new HttpEntity<>(request),
				new ParameterizedTypeReference<Result<TaskStatusAckDTO>>() {
				}
		);
		Result<TaskStatusAckDTO> result = response.getBody();
		ensureSuccess(result, "mark task dispatch failed");
		TaskStatusAckDTO responseData = requireData(result, "mark task dispatch failed");
		if (responseData.getTaskId() == null
				|| responseData.getStatus() == null
				|| responseData.getStatus().isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "mark task dispatch failed response data is invalid");
		}
		return responseData;
	}

	@Override
	public int markNodeOfflineTasksFailed(Long nodeId, String reason) {
		String url = taskServiceBaseUrl + "/internal/tasks/node-offline/fail";
		java.util.Map<String, Object> request = new java.util.HashMap<>();
		request.put("nodeId", nodeId);
		request.put("reason", reason);
		ResponseEntity<Result<Integer>> response = restTemplate.exchange(
				url,
				HttpMethod.POST,
				new HttpEntity<>(request),
				new ParameterizedTypeReference<Result<Integer>>() {
				}
		);
		Result<Integer> result = response.getBody();
		ensureSuccess(result, "mark node offline tasks failed");
		return requireData(result, "mark node offline tasks failed");
	}

	private void ensureSuccess(Result<?> result, String action) {
		if (result == null) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response is empty");
		}
		if (result.getCode() != null && result.getCode() != 0) {
			throw new BizException(result.getCode(), result.getMessage(), result.getData());
		}
	}

	private <T> T requireData(Result<T> result, String action) {
		if (result == null || result.getData() == null) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response data is empty");
		}
		return result.getData();
	}
}
