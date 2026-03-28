package com.example.cae.task.infrastructure.client;

import com.example.cae.common.response.Result;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SchedulerClient {
	private final RestTemplate restTemplate;
	private final String schedulerServiceBaseUrl;

	public SchedulerClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		this.schedulerServiceBaseUrl = System.getProperty("scheduler.service.base-url", "http://localhost:8084");
	}

	public void notifyTaskSubmitted(Long taskId) {
		// reserved for async dispatch integration
	}

	@SuppressWarnings("unchecked")
	public boolean verifyNodeToken(Long nodeId, String nodeToken) {
		String url = UriComponentsBuilder
				.fromHttpUrl(schedulerServiceBaseUrl + "/internal/nodes/{nodeId}/token/verify")
				.queryParam("nodeToken", nodeToken)
				.buildAndExpand(nodeId)
				.toUriString();
		Result<Object> result = restTemplate.getForObject(url, Result.class);
		if (result == null || result.getData() == null) {
			return false;
		}
		Object data = result.getData();
		if (data instanceof Boolean bool) {
			return bool;
		}
		return Boolean.parseBoolean(String.valueOf(data));
	}
}

