package com.example.cae.task.infrastructure.client;

import com.example.cae.common.response.Result;
import com.example.cae.task.config.TaskRemoteServiceProperties;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
public class SchedulerClient {
	private final RestTemplate restTemplate;
	private final String schedulerServiceBaseUrl;

	public SchedulerClient(RestTemplate restTemplate, TaskRemoteServiceProperties remoteServiceProperties) {
		this.restTemplate = restTemplate;
		this.schedulerServiceBaseUrl = remoteServiceProperties.getSchedulerBaseUrl();
	}

	public void notifyTaskSubmitted(Long taskId) {
		// reserved for async dispatch integration
	}

	public void cancelTaskOnNode(Long nodeId, Long taskId, String reason) {
		String url = schedulerServiceBaseUrl + "/internal/nodes/" + nodeId + "/cancel-task";
		Map<String, Object> body = new java.util.HashMap<>();
		body.put("taskId", taskId);
		body.put("reason", reason);
		restTemplate.postForEntity(url, body, Result.class);
	}

	@SuppressWarnings("unchecked")
	public String getNodeName(Long nodeId) {
		if (nodeId == null) {
			return null;
		}
		String url = schedulerServiceBaseUrl + "/api/nodes/" + nodeId;
		Result<Object> result = restTemplate.getForObject(url, Result.class);
		if (result == null || !(result.getData() instanceof java.util.Map<?, ?> map)) {
			return null;
		}
		return String.valueOf(map.get("nodeName"));
	}

	public QueueNodeSnapshot getQueueNodeSnapshot(Long solverId) {
		if (solverId == null) {
			return QueueNodeSnapshot.empty();
		}
		QueueNodeSnapshot snapshot = new QueueNodeSnapshot();
		snapshot.setDispatchableNodeCount(fetchDispatchableNodeCount(solverId));
		snapshot.setOnlineEnabledCapableNodeCount(fetchOnlineEnabledCapableNodeCount(solverId));
		return snapshot;
	}

	@SuppressWarnings("unchecked")
	private int fetchDispatchableNodeCount(Long solverId) {
		String availableUrl = UriComponentsBuilder
				.fromHttpUrl(schedulerServiceBaseUrl + "/internal/nodes/available")
				.queryParam("solverId", solverId)
				.toUriString();
		Result<Object> availableResult = restTemplate.getForObject(availableUrl, Result.class);
		if (availableResult == null || !(availableResult.getData() instanceof List<?> records)) {
			return 0;
		}
		return (int) records.stream().filter(Map.class::isInstance).count();
	}

	@SuppressWarnings("unchecked")
	private int fetchOnlineEnabledCapableNodeCount(Long solverId) {
		String onlineUrl = UriComponentsBuilder
				.fromHttpUrl(schedulerServiceBaseUrl + "/api/nodes")
				.queryParam("status", "ONLINE")
				.queryParam("enabled", 1)
				.queryParam("solverId", solverId)
				.queryParam("pageNum", 1)
				.queryParam("pageSize", 1)
				.toUriString();
		Result<Object> result = restTemplate.getForObject(onlineUrl, Result.class);
		if (result == null || !(result.getData() instanceof Map<?, ?> pageMap)) {
			return 0;
		}
		return toInteger(pageMap.get("total")) == null ? 0 : toInteger(pageMap.get("total"));
	}

	@SuppressWarnings("unchecked")
	public NodeSummary getOnlineNodeSummary() {
		String url = UriComponentsBuilder
				.fromHttpUrl(schedulerServiceBaseUrl + "/api/nodes")
				.queryParam("status", "ONLINE")
				.queryParam("pageNum", 1)
				.queryParam("pageSize", 1000)
				.toUriString();
		Result<Object> result = restTemplate.getForObject(url, Result.class);
		if (result == null || !(result.getData() instanceof Map<?, ?> pageMap)) {
			return NodeSummary.empty();
		}
		Object recordsObj = pageMap.get("records");
		if (!(recordsObj instanceof List<?> records)) {
			return NodeSummary.empty();
		}
		int onlineCount = 0;
		BigDecimal totalLoad = BigDecimal.ZERO;
		int loadSamples = 0;
		for (Object item : records) {
			if (!(item instanceof Map<?, ?> nodeMap)) {
				continue;
			}
			onlineCount++;
			Integer runningCount = toInteger(nodeMap.get("runningCount"));
			Integer maxConcurrency = toInteger(nodeMap.get("maxConcurrency"));
			if (runningCount != null && maxConcurrency != null && maxConcurrency > 0) {
				totalLoad = totalLoad.add(BigDecimal.valueOf(runningCount)
						.divide(BigDecimal.valueOf(maxConcurrency), 4, RoundingMode.HALF_UP));
				loadSamples++;
			}
		}
		NodeSummary summary = new NodeSummary();
		summary.setOnlineNodeCount(onlineCount);
		summary.setAvgNodeLoad(loadSamples == 0
				? BigDecimal.ZERO
				: totalLoad.divide(BigDecimal.valueOf(loadSamples), 4, RoundingMode.HALF_UP));
		return summary;
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

	private Integer toInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		return Integer.parseInt(String.valueOf(value));
	}

	public static class NodeSummary {
		private Integer onlineNodeCount;
		private BigDecimal avgNodeLoad;

		public static NodeSummary empty() {
			NodeSummary summary = new NodeSummary();
			summary.setOnlineNodeCount(0);
			summary.setAvgNodeLoad(BigDecimal.ZERO);
			return summary;
		}

		public Integer getOnlineNodeCount() {
			return onlineNodeCount;
		}

		public void setOnlineNodeCount(Integer onlineNodeCount) {
			this.onlineNodeCount = onlineNodeCount;
		}

		public BigDecimal getAvgNodeLoad() {
			return avgNodeLoad;
		}

		public void setAvgNodeLoad(BigDecimal avgNodeLoad) {
			this.avgNodeLoad = avgNodeLoad;
		}
	}

	public static class QueueNodeSnapshot {
		private Integer dispatchableNodeCount;
		private Integer onlineEnabledCapableNodeCount;

		public static QueueNodeSnapshot empty() {
			QueueNodeSnapshot snapshot = new QueueNodeSnapshot();
			snapshot.setDispatchableNodeCount(0);
			snapshot.setOnlineEnabledCapableNodeCount(0);
			return snapshot;
		}

		public Integer getDispatchableNodeCount() {
			return dispatchableNodeCount;
		}

		public void setDispatchableNodeCount(Integer dispatchableNodeCount) {
			this.dispatchableNodeCount = dispatchableNodeCount;
		}

		public Integer getOnlineEnabledCapableNodeCount() {
			return onlineEnabledCapableNodeCount;
		}

		public void setOnlineEnabledCapableNodeCount(Integer onlineEnabledCapableNodeCount) {
			this.onlineEnabledCapableNodeCount = onlineEnabledCapableNodeCount;
		}
	}
}
