package com.example.cae.task.infrastructure.client;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.Result;
import com.example.cae.task.config.TaskRemoteServiceProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
		Result<?> result = restTemplate.postForObject(url, body, Result.class);
		ensureSuccess(result, "cancel task on node");
	}

	public NodeReservationActionResult releaseNodeReservation(Long nodeId, Long taskId) {
		if (nodeId == null || taskId == null) {
			return null;
		}
		String url = schedulerServiceBaseUrl + "/internal/nodes/" + nodeId + "/release-reservation";
		Map<String, Object> body = new java.util.HashMap<>();
		body.put("taskId", taskId);
		Result<NodeReservationActionResult> result = restTemplate.exchange(
				url,
				HttpMethod.POST,
				new HttpEntity<>(body),
				new ParameterizedTypeReference<Result<NodeReservationActionResult>>() {
				}
		).getBody();
		ensureSuccess(result, "release node reservation");
		NodeReservationActionResult actionResult = result == null ? null : result.getData();
		if (actionResult == null
				|| actionResult.getTaskId() == null
				|| actionResult.getNodeId() == null
				|| actionResult.getReservationStatus() == null
				|| actionResult.getReservationStatus().isBlank()
				|| actionResult.getReservedCount() == null) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "release node reservation response data is invalid");
		}
		if (!taskId.equals(actionResult.getTaskId()) || !nodeId.equals(actionResult.getNodeId())) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "release node reservation response identity mismatch");
		}
		return actionResult;
	}

	@SuppressWarnings("unchecked")
	public String getNodeName(Long nodeId) {
		if (nodeId == null) {
			return null;
		}
		String url = schedulerServiceBaseUrl + "/api/nodes/" + nodeId;
		Result<Object> result = restTemplate.getForObject(url, Result.class);
		ensureSuccess(result, "get node name");
		Map<String, Object> map = requireMapData(result, "get node name");
		return toString(map.get("nodeName"));
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
		ensureSuccess(availableResult, "fetch dispatchable node count");
		List<?> records = requireListData(availableResult, "fetch dispatchable node count");
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
		ensureSuccess(result, "fetch online enabled capable node count");
		Map<String, Object> pageMap = requireMapData(result, "fetch online enabled capable node count");
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
		ensureSuccess(result, "get online node summary");
		Map<String, Object> pageMap = requireMapData(result, "get online node summary");
		Object recordsObj = pageMap.get("records");
		if (!(recordsObj instanceof List<?> records)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "get online node summary response records is invalid");
		}
		int onlineCount = 0;
		BigDecimal totalLoad = BigDecimal.ZERO;
		int loadSamples = 0;
		for (Object item : records) {
			if (!(item instanceof Map<?, ?> nodeMap)) {
				continue;
			}
			onlineCount++;
			Integer enabled = toInteger(nodeMap.get("enabled"));
			if (enabled == null || enabled != 1) {
				continue;
			}
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
	public List<ScheduleRecordItem> listTaskScheduleRecords(Long taskId) {
		if (taskId == null) {
			return List.of();
		}
		String url = schedulerServiceBaseUrl + "/internal/tasks/" + taskId + "/schedules";
		Result<Object> result = restTemplate.getForObject(url, Result.class);
		ensureSuccess(result, "list task schedule records");
		List<?> rows = requireListData(result, "list task schedule records");
		List<ScheduleRecordItem> records = new ArrayList<>();
		for (Object row : rows) {
			if (!(row instanceof Map<?, ?> map)) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "list task schedule records response row is invalid");
			}
			ScheduleRecordItem item = new ScheduleRecordItem();
			item.setScheduleId(toLong(map.get("scheduleId")));
			item.setTaskId(toLong(map.get("taskId")));
			item.setTaskNo(toString(map.get("taskNo")));
			item.setNodeId(toLong(map.get("nodeId")));
			item.setNodeName(toString(map.get("nodeName")));
			item.setStrategyName(toString(map.get("strategyName")));
			item.setScheduleStatus(toString(map.get("scheduleStatus")));
			item.setScheduleMessage(toString(map.get("scheduleMessage")));
			item.setCreatedAt(toLocalDateTime(map.get("createdAt")));
			records.add(item);
		}
		return records;
	}

	@SuppressWarnings("unchecked")
	public boolean verifyNodeToken(Long nodeId, String nodeToken) {
		String url = UriComponentsBuilder
				.fromHttpUrl(schedulerServiceBaseUrl + "/internal/nodes/{nodeId}/token/verify")
				.queryParam("nodeToken", nodeToken)
				.buildAndExpand(nodeId)
				.toUriString();
		Result<Object> result = restTemplate.getForObject(url, Result.class);
		ensureSuccess(result, "verify node token");
		if (result == null || result.getData() == null) {
			return false;
		}
		Object data = result.getData();
		if (data instanceof Boolean bool) {
			return bool;
		}
		return Boolean.parseBoolean(String.valueOf(data));
	}

	private void ensureSuccess(Result<?> result, String action) {
		if (result == null) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response is empty");
		}
		if (result.getCode() != null && result.getCode() != 0) {
			throw new BizException(result.getCode(), result.getMessage(), result.getData());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> requireMapData(Result<?> result, String action) {
		if (!(result.getData() instanceof Map<?, ?> map)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response data is invalid");
		}
		return (Map<String, Object>) map;
	}

	private List<?> requireListData(Result<?> result, String action) {
		if (!(result.getData() instanceof List<?> rows)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response data is invalid");
		}
		return rows;
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

	private Long toLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Long.parseLong(String.valueOf(value));
	}

	private String toString(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private LocalDateTime toLocalDateTime(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return LocalDateTime.parse(String.valueOf(value));
		} catch (Exception ex) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "response datetime is invalid: " + value);
		}
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

	public static class ScheduleRecordItem {
		private Long scheduleId;
		private Long taskId;
		private String taskNo;
		private Long nodeId;
		private String nodeName;
		private String strategyName;
		private String scheduleStatus;
		private String scheduleMessage;
		private LocalDateTime createdAt;

		public Long getScheduleId() {
			return scheduleId;
		}

		public void setScheduleId(Long scheduleId) {
			this.scheduleId = scheduleId;
		}

		public Long getTaskId() {
			return taskId;
		}

		public void setTaskId(Long taskId) {
			this.taskId = taskId;
		}

		public String getTaskNo() {
			return taskNo;
		}

		public void setTaskNo(String taskNo) {
			this.taskNo = taskNo;
		}

		public Long getNodeId() {
			return nodeId;
		}

		public void setNodeId(Long nodeId) {
			this.nodeId = nodeId;
		}

		public String getNodeName() {
			return nodeName;
		}

		public void setNodeName(String nodeName) {
			this.nodeName = nodeName;
		}

		public String getStrategyName() {
			return strategyName;
		}

		public void setStrategyName(String strategyName) {
			this.strategyName = strategyName;
		}

		public String getScheduleStatus() {
			return scheduleStatus;
		}

		public void setScheduleStatus(String scheduleStatus) {
			this.scheduleStatus = scheduleStatus;
		}

		public String getScheduleMessage() {
			return scheduleMessage;
		}

		public void setScheduleMessage(String scheduleMessage) {
			this.scheduleMessage = scheduleMessage;
		}

		public LocalDateTime getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(LocalDateTime createdAt) {
			this.createdAt = createdAt;
		}
	}

	public static class NodeReservationActionResult {
		private Long taskId;
		private Long nodeId;
		private String reservationStatus;
		private Integer reservedCount;

		public Long getTaskId() {
			return taskId;
		}

		public void setTaskId(Long taskId) {
			this.taskId = taskId;
		}

		public Long getNodeId() {
			return nodeId;
		}

		public void setNodeId(Long nodeId) {
			this.nodeId = nodeId;
		}

		public String getReservationStatus() {
			return reservationStatus;
		}

		public void setReservationStatus(String reservationStatus) {
			this.reservationStatus = reservationStatus;
		}

		public Integer getReservedCount() {
			return reservedCount;
		}

		public void setReservedCount(Integer reservedCount) {
			this.reservedCount = reservedCount;
		}
	}
}
