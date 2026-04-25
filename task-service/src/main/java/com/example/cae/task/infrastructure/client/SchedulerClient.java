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
	private static final int NODE_PAGE_SIZE = 200;
	private static final int SCHEDULE_MESSAGE_MAX_LENGTH = 255;
	private static final String DEFAULT_STRATEGY_NAME = "FCFS_MIN_LOAD";
	private final RestTemplate restTemplate;
	private final String schedulerServiceBaseUrl;

	public SchedulerClient(RestTemplate restTemplate, TaskRemoteServiceProperties remoteServiceProperties) {
		this.restTemplate = restTemplate;
		this.schedulerServiceBaseUrl = remoteServiceProperties.getSchedulerBaseUrl();
	}

	public void notifyTaskSubmitted(Long taskId) {
		// reserved for async dispatch integration
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

	public void recordScheduleFailure(Long taskId, Long nodeId, String scheduleMessage) {
		recordSchedule(taskId, nodeId, "FAILED", scheduleMessage);
	}

	private void recordSchedule(Long taskId, Long nodeId, String scheduleStatus, String scheduleMessage) {
		if (taskId == null) {
			return;
		}
		String url = schedulerServiceBaseUrl + "/internal/schedules";
		Map<String, Object> body = new java.util.HashMap<>();
		body.put("taskId", taskId);
		body.put("nodeId", nodeId);
		body.put("strategyName", DEFAULT_STRATEGY_NAME);
		body.put("scheduleStatus", scheduleStatus);
		body.put("scheduleMessage", truncateScheduleMessage(scheduleMessage));
		Result<Void> result = restTemplate.exchange(
				url,
				HttpMethod.POST,
				new HttpEntity<>(body),
				new ParameterizedTypeReference<Result<Void>>() {
				}
		).getBody();
		ensureSuccess(result, "record schedule failure");
	}

	private String truncateScheduleMessage(String scheduleMessage) {
		String message = scheduleMessage == null || scheduleMessage.isBlank()
				? "reservation release failed"
				: scheduleMessage.trim();
		return message.length() <= SCHEDULE_MESSAGE_MAX_LENGTH
				? message
				: message.substring(0, SCHEDULE_MESSAGE_MAX_LENGTH);
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
		String nodeName = toString(map.get("nodeName"));
		if (nodeName == null || nodeName.isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "get node name response data is invalid");
		}
		return nodeName;
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
		int count = 0;
		for (Object record : records) {
			if (!(record instanceof Map<?, ?> nodeMap)) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "fetch dispatchable node count response row is invalid");
			}
			Long nodeId = toLong(nodeMap.get("nodeId"));
			Integer runningCount = toInteger(nodeMap.get("runningCount"));
			Integer reservedCount = toInteger(nodeMap.get("reservedCount"));
			Integer maxConcurrency = toInteger(nodeMap.get("maxConcurrency"));
			if (nodeId == null || runningCount == null || reservedCount == null || maxConcurrency == null) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "fetch dispatchable node count response row is incomplete");
			}
			count++;
		}
		return count;
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
		Integer total = toInteger(pageMap.get("total"));
		if (total == null) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "fetch online enabled capable node count response total is invalid");
		}
		Object recordsObj = pageMap.get("records");
		if (!(recordsObj instanceof List<?> records)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "fetch online enabled capable node count response records is invalid");
		}
		for (Object item : records) {
			if (!(item instanceof Map<?, ?> nodeMap)) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "fetch online enabled capable node count response row is invalid");
			}
			Long nodeId = toLong(nodeMap.get("nodeId"));
			Integer enabled = toInteger(nodeMap.get("enabled"));
			Integer maxConcurrency = toInteger(nodeMap.get("maxConcurrency"));
			if (nodeId == null || enabled == null || maxConcurrency == null) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "fetch online enabled capable node count response row is incomplete");
			}
		}
		return total;
	}

	@SuppressWarnings("unchecked")
	public NodeSummary getOnlineNodeSummary() {
		List<Map<String, Object>> records = fetchAllNodePages("ONLINE", null, null, "get online node summary");
		int onlineCount = 0;
		BigDecimal totalLoad = BigDecimal.ZERO;
		int loadSamples = 0;
		for (Map<String, Object> nodeMap : records) {
			Long nodeId = toLong(nodeMap.get("nodeId"));
			Integer enabled = toInteger(nodeMap.get("enabled"));
			Integer runningCount = toInteger(nodeMap.get("runningCount"));
			Integer maxConcurrency = toInteger(nodeMap.get("maxConcurrency"));
			if (nodeId == null || enabled == null || runningCount == null || maxConcurrency == null) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "get online node summary response row is incomplete");
			}
			onlineCount++;
			if (enabled != 1) {
				continue;
			}
			if (maxConcurrency > 0) {
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
	private List<Map<String, Object>> fetchAllNodePages(String status, Integer enabled, Long solverId, String action) {
		List<Map<String, Object>> allRecords = new ArrayList<>();
		int pageNum = 1;
		Integer total = null;
		while (total == null || allRecords.size() < total) {
			String url = UriComponentsBuilder
					.fromHttpUrl(schedulerServiceBaseUrl + "/api/nodes")
					.queryParam("pageNum", pageNum)
					.queryParam("pageSize", NODE_PAGE_SIZE)
					.queryParamIfPresent("status", java.util.Optional.ofNullable(status))
					.queryParamIfPresent("enabled", java.util.Optional.ofNullable(enabled))
					.queryParamIfPresent("solverId", java.util.Optional.ofNullable(solverId))
					.toUriString();
			Result<Object> result = restTemplate.getForObject(url, Result.class);
			ensureSuccess(result, action);
			Map<String, Object> pageMap = requireMapData(result, action);
			Integer pageTotal = toInteger(pageMap.get("total"));
			if (pageTotal == null) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response total is invalid");
			}
			total = pageTotal;
			Object recordsObj = pageMap.get("records");
			if (!(recordsObj instanceof List<?> records)) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response records is invalid");
			}
			for (Object item : records) {
				if (!(item instanceof Map<?, ?> nodeMap)) {
					throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response row is invalid");
				}
				allRecords.add((Map<String, Object>) nodeMap);
			}
			if (records.isEmpty()) {
				break;
			}
			pageNum++;
		}
		return allRecords;
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
			validateScheduleRecordItem(item, taskId);
			records.add(item);
		}
		return records;
	}

	private void validateScheduleRecordItem(ScheduleRecordItem item, Long expectedTaskId) {
		if (item == null
				|| item.getScheduleId() == null
				|| item.getTaskId() == null
				|| item.getTaskNo() == null
				|| item.getTaskNo().isBlank()
				|| item.getStrategyName() == null
				|| item.getStrategyName().isBlank()
				|| item.getScheduleStatus() == null
				|| item.getScheduleStatus().isBlank()
				|| item.getScheduleMessage() == null
				|| item.getCreatedAt() == null) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "list task schedule records response item is incomplete");
		}
		if (expectedTaskId != null && !expectedTaskId.equals(item.getTaskId())) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "list task schedule records response taskId mismatch");
		}
		if (item.getNodeId() == null) {
			return;
		}
		if (item.getNodeName() == null || item.getNodeName().isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "list task schedule records response nodeName is invalid");
		}
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
