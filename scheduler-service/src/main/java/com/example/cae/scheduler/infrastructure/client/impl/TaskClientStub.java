package com.example.cae.scheduler.infrastructure.client.impl;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class TaskClientStub implements TaskClient {
	private final RestTemplate restTemplate;
	private final String taskServiceBaseUrl;

	public TaskClientStub(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		this.taskServiceBaseUrl = System.getProperty("task.service.base-url", "http://localhost:8083");
	}

	@Override
	public List<TaskDTO> listPendingTasks() {
		String url = taskServiceBaseUrl + "/internal/tasks/queued";
		ResponseEntity<Result<List<TaskDTO>>> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<Result<List<TaskDTO>>>() {
				}
		);
		Result<List<TaskDTO>> body = response.getBody();
		if (body == null || body.getData() == null) {
			return List.of();
		}
		return body.getData();
	}

	@Override
	public void markTaskScheduled(Long taskId, Long nodeId) {
		String url = taskServiceBaseUrl + "/internal/tasks/" + taskId + "/mark-scheduled?nodeId=" + nodeId;
		restTemplate.postForEntity(url, null, Result.class);
	}

	@Override
	public void markTaskDispatched(Long taskId, Long nodeId) {
		String url = taskServiceBaseUrl + "/internal/tasks/" + taskId + "/mark-dispatched?nodeId=" + nodeId;
		restTemplate.postForEntity(url, null, Result.class);
	}
}
