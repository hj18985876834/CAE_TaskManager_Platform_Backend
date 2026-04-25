package com.example.cae.scheduler.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskBasicDTO;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.dto.TaskStatusAckDTO;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.manager.NodeCapacityManager;
import com.example.cae.scheduler.application.assembler.ScheduleAssembler;
import com.example.cae.scheduler.domain.enums.ScheduleStatusEnum;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeSolverCapability;
import com.example.cae.scheduler.domain.model.ScheduleRecord;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.domain.repository.ScheduleRecordRepository;
import com.example.cae.scheduler.domain.service.ScheduleDomainService;
import com.example.cae.scheduler.domain.strategy.ScheduleStrategy;
import com.example.cae.scheduler.infrastructure.client.SolverClient;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import com.example.cae.scheduler.interfaces.request.InternalScheduleRecordRequest;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;
import com.example.cae.scheduler.interfaces.response.NodeReservationActionResponse;
import com.example.cae.scheduler.interfaces.response.ScheduleRecordResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScheduleAppService {
	private static final Set<String> DISPATCH_FAILURE_RELEASE_TARGETS = Set.of("QUEUED", "FAILED");
	private static final int SCHEDULE_MESSAGE_MAX_LENGTH = 255;
	private static final String NO_ONLINE_NODE_MESSAGE = "no online node";
	private static final String NO_ENABLED_CAPABILITY_MESSAGE = "no enabled node solver capability";
	private static final String NO_DISPATCHABLE_NODE_MESSAGE = "no dispatchable node with capacity";
	private static final String CAPACITY_CONFLICT_MESSAGE = "candidate nodes are full or reservation conflicted";
	private final ComputeNodeRepository computeNodeRepository;
	private final NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
	private final ScheduleRecordRepository scheduleRecordRepository;
	private final ScheduleDomainService scheduleDomainService;
	private final ScheduleStrategy scheduleStrategy;
	private final SolverClient solverClient;
	private final TaskClient taskClient;
	private final NodeCapacityManager nodeCapacityManager;

	public ScheduleAppService(ComputeNodeRepository computeNodeRepository,
							NodeSolverCapabilityRepository nodeSolverCapabilityRepository,
							ScheduleRecordRepository scheduleRecordRepository,
							ScheduleDomainService scheduleDomainService,
							ScheduleStrategy scheduleStrategy,
							SolverClient solverClient,
							TaskClient taskClient,
							NodeCapacityManager nodeCapacityManager) {
		this.computeNodeRepository = computeNodeRepository;
		this.nodeSolverCapabilityRepository = nodeSolverCapabilityRepository;
		this.scheduleRecordRepository = scheduleRecordRepository;
		this.scheduleDomainService = scheduleDomainService;
		this.scheduleStrategy = scheduleStrategy;
		this.solverClient = solverClient;
		this.taskClient = taskClient;
		this.nodeCapacityManager = nodeCapacityManager;
	}

	@Transactional
	public Long scheduleTask(TaskDTO task) {
		if (task == null || task.getTaskId() == null || task.getSolverId() == null || task.getProfileId() == null) {
			throw new BizException(ErrorCodeConstants.INVALID_SCHEDULE_TASK, "invalid task for scheduling");
		}
		validateSolverAndProfile(task);

		List<ComputeNode> onlineNodes = computeNodeRepository.listByStatus("ONLINE");
		List<NodeSolverCapability> solverCapabilities = nodeSolverCapabilityRepository.listBySolverId(task.getSolverId());
		List<ComputeNode> availableNodes = scheduleDomainService.filterAvailableNodes(
				onlineNodes,
				task.getSolverId(),
				solverCapabilities
		);

		List<ComputeNode> orderedCandidates = scheduleStrategy.orderNodes(task, availableNodes);
		if (orderedCandidates.isEmpty()) {
			throw new BizException(ErrorCodeConstants.NO_AVAILABLE_NODE, CAPACITY_CONFLICT_MESSAGE);
		}

		BizException lastCapacityFailure = null;
		for (ComputeNode candidate : orderedCandidates) {
			if (candidate == null || candidate.getId() == null) {
				continue;
			}
			try {
				NodeReservationActionResponse reservation = nodeCapacityManager.reserve(candidate.getId(), task.getTaskId());
				return reservation.getNodeId();
			} catch (BizException ex) {
				if (ex.getCode() == null || ex.getCode() != ErrorCodeConstants.NO_AVAILABLE_NODE) {
					throw ex;
				}
				lastCapacityFailure = ex;
			}
		}

		if (lastCapacityFailure != null) {
			throw new BizException(ErrorCodeConstants.NO_AVAILABLE_NODE, CAPACITY_CONFLICT_MESSAGE);
		}
		throw new BizException(ErrorCodeConstants.NO_AVAILABLE_NODE, CAPACITY_CONFLICT_MESSAGE);
	}

	private String buildNoAvailableNodeMessage(TaskDTO task,
											 List<ComputeNode> onlineNodes,
											 List<NodeSolverCapability> solverCapabilities,
											 List<ComputeNode> availableNodes) {
		if (onlineNodes == null || onlineNodes.isEmpty()) {
			return NO_ONLINE_NODE_MESSAGE;
		}
		boolean hasEnabledCapability = solverCapabilities != null && solverCapabilities.stream()
				.anyMatch(capability -> capability != null
						&& capability.isEnabled()
						&& Objects.equals(capability.getSolverId(), task.getSolverId()));
		if (!hasEnabledCapability) {
			return NO_ENABLED_CAPABILITY_MESSAGE;
		}
		if (availableNodes == null || availableNodes.isEmpty()) {
			return NO_DISPATCHABLE_NODE_MESSAGE;
		}
		return CAPACITY_CONFLICT_MESSAGE;
	}

	private void validateSolverAndProfile(TaskDTO task) {
		SolverClient.SolverMeta solverMeta = solverClient.getSolverMeta(task.getSolverId());
		if (solverMeta == null || solverMeta.getSolverId() == null) {
			throw new BizException(ErrorCodeConstants.SOLVER_NOT_FOUND, "solver not found: " + task.getSolverId());
		}
		if (!solverMeta.isEnabled()) {
			throw new BizException(
					ErrorCodeConstants.SOLVER_DISABLED,
					"solver is disabled: " + (solverMeta.getSolverName() == null ? task.getSolverId() : solverMeta.getSolverName())
			);
		}

		SolverClient.ProfileMeta profileMeta = solverClient.getProfileMeta(task.getProfileId());
		if (profileMeta == null || profileMeta.getProfileId() == null) {
			throw new BizException(ErrorCodeConstants.PROFILE_NOT_FOUND, "profile not found: " + task.getProfileId());
		}
		if (!profileMeta.isEnabled()) {
			throw new BizException(
					ErrorCodeConstants.PROFILE_DISABLED,
					"profile is disabled: " + (profileMeta.getProfileName() == null ? task.getProfileId() : profileMeta.getProfileName())
			);
		}
		if (!task.getSolverId().equals(profileMeta.getSolverId())) {
			throw new BizException(ErrorCodeConstants.TASK_PROFILE_MISMATCH, "solver and profile do not match");
		}
		if (profileMeta.getTaskType() != null && !profileMeta.getTaskType().isBlank()
				&& task.getTaskType() != null && !profileMeta.getTaskType().equals(task.getTaskType())) {
			throw new BizException(ErrorCodeConstants.TASK_TYPE_MISMATCH, "task type and profile do not match");
		}
	}

	public void confirmScheduleSuccess(Long taskId, Long nodeId, String scheduleMessage) {
		ScheduleRecord success = ScheduleAssembler.newRecord(
				taskId,
				nodeId,
				"FCFS_MIN_LOAD",
				"SUCCESS",
				normalizeScheduleMessage(scheduleMessage, "dispatch success")
		);
		scheduleRecordRepository.save(success);
	}

	public void recordScheduleFailure(Long taskId, Long nodeId, String scheduleMessage) {
		ScheduleRecord failure = ScheduleAssembler.newRecord(
				taskId,
				nodeId,
				"FCFS_MIN_LOAD",
				"FAILED",
				normalizeScheduleMessage(scheduleMessage, "dispatch failed")
		);
		scheduleRecordRepository.save(failure);
	}

	@Transactional
	public TaskStatusAckDTO handleDispatchFailure(Long taskId, Long nodeId, String failType, String reason, boolean recoverable) {
		if (taskId == null || nodeId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "taskId and nodeId are required");
		}
		TaskStatusAckDTO ack = taskClient.markTaskFailed(taskId, nodeId, failType, reason, recoverable);
		if (ack != null && DISPATCH_FAILURE_RELEASE_TARGETS.contains(ack.getStatus())) {
			releaseNodeReservation(nodeId, taskId);
			recordScheduleFailure(taskId, nodeId, reason);
		}
		return ack;
	}

	@Transactional
	public void releaseNodeReservation(Long nodeId, Long taskId) {
		if (nodeId == null || taskId == null) {
			return;
		}
		nodeCapacityManager.release(nodeId, taskId);
	}

	public PageResult<ScheduleRecordResponse> pageRecords(SchedulePageQueryRequest request) {
		SchedulePageQueryRequest query = sanitizePageQueryRequest(request);
		int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
		int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();
		long offset = (long) (pageNum - 1) * pageSize;

		PageResult<ScheduleRecord> page = scheduleRecordRepository.page(query, offset, pageSize);
		List<ScheduleRecordResponse> records = page.getRecords().stream()
				.map(ScheduleAssembler::toResponse)
				.toList();
		enrichScheduleRecordResponses(records);
		validateScheduleRecordResponses(records);
		return PageResult.of(page.getTotal(), pageNum, pageSize, records);
	}

	public void recordSchedule(InternalScheduleRecordRequest request) {
		if (request == null || request.getTaskId() == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid schedule record request");
		}
		String strategyName = request.getStrategyName();
		if (strategyName == null || strategyName.isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "strategyName is required");
		}
		ScheduleRecord record = ScheduleAssembler.newRecord(
				request.getTaskId(),
				request.getNodeId(),
				strategyName,
				normalizeScheduleStatus(request.getScheduleStatus()),
				normalizeScheduleMessage(request.getScheduleMessage(), null)
		);
		scheduleRecordRepository.save(record);
	}

	private String normalizeScheduleMessage(String scheduleMessage, String defaultMessage) {
		String message = scheduleMessage == null || scheduleMessage.isBlank() ? defaultMessage : scheduleMessage.trim();
		if (message == null) {
			return null;
		}
		return message.length() <= SCHEDULE_MESSAGE_MAX_LENGTH
				? message
				: message.substring(0, SCHEDULE_MESSAGE_MAX_LENGTH);
	}

	public List<ScheduleRecordResponse> listByTaskId(Long taskId) {
		if (taskId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "taskId is required");
		}
		List<ScheduleRecordResponse> records = scheduleRecordRepository.listByTaskId(taskId).stream()
				.map(ScheduleAssembler::toResponse)
				.toList();
		enrichScheduleRecordResponses(records);
		validateScheduleRecordResponses(records);
		return records;
	}

	private void enrichScheduleRecordResponses(List<ScheduleRecordResponse> records) {
		if (records == null || records.isEmpty()) {
			return;
		}
		Map<Long, TaskBasicDTO> taskBasics = fetchTaskBasics(records);
		for (ScheduleRecordResponse response : records) {
			if (response == null) {
				continue;
			}
			if (response.getTaskId() != null) {
				TaskBasicDTO taskBasic = taskBasics.get(response.getTaskId());
				if (taskBasic != null) {
					response.setTaskNo(taskBasic.getTaskNo());
				}
			}
			if (response.getNodeId() != null) {
				ComputeNode node = computeNodeRepository.findById(response.getNodeId())
						.orElseThrow(() -> new BizException(
								ErrorCodeConstants.NODE_NOT_FOUND,
								"schedule record node not found: " + response.getNodeId()
						));
				response.setNodeName(node.getNodeName());
			}
		}
	}

	private Map<Long, TaskBasicDTO> fetchTaskBasics(List<ScheduleRecordResponse> records) {
		List<Long> taskIds = records.stream()
				.filter(response -> response != null && response.getTaskId() != null)
				.map(ScheduleRecordResponse::getTaskId)
				.distinct()
				.collect(Collectors.toList());
		if (taskIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, TaskBasicDTO> taskBasics = taskClient.getTaskBasics(taskIds);
		for (Long taskId : taskIds) {
			TaskBasicDTO taskBasic = taskBasics.get(taskId);
			if (taskBasic == null || taskBasic.getTaskNo() == null || taskBasic.getTaskNo().isBlank()) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY,
						"schedule record task basic is missing or invalid: " + taskId);
			}
		}
		return taskBasics;
	}

	private void validateScheduleRecordResponses(List<ScheduleRecordResponse> records) {
		if (records == null || records.isEmpty()) {
			return;
		}
		for (ScheduleRecordResponse response : records) {
			if (response == null
					|| response.getScheduleId() == null
					|| response.getTaskId() == null
					|| response.getTaskNo() == null
					|| response.getTaskNo().isBlank()
					|| response.getStrategyName() == null
					|| response.getStrategyName().isBlank()
					|| response.getScheduleStatus() == null
					|| response.getScheduleStatus().isBlank()
					|| response.getScheduleMessage() == null
					|| response.getCreatedAt() == null) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "schedule record response is incomplete");
			}
			if (response.getNodeId() != null
					&& (response.getNodeName() == null || response.getNodeName().isBlank())) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "schedule record nodeName is invalid");
			}
		}
	}

	private String normalizeScheduleStatus(String scheduleStatus) {
		if (scheduleStatus == null || scheduleStatus.isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "scheduleStatus is required");
		}
		try {
			return ScheduleStatusEnum.valueOf(scheduleStatus.trim().toUpperCase(Locale.ROOT)).name();
		} catch (IllegalArgumentException ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid scheduleStatus: " + scheduleStatus);
		}
	}

	private SchedulePageQueryRequest sanitizePageQueryRequest(SchedulePageQueryRequest request) {
		if (request == null) {
			request = new SchedulePageQueryRequest();
		}
		request.setScheduleStatus(normalizeScheduleStatusFilter(request.getScheduleStatus()));
		validateTimeRange(request.getStartTime(), request.getEndTime());
		return request;
	}

	private String normalizeScheduleStatusFilter(String scheduleStatus) {
		String normalized = normalizeBlankToNull(scheduleStatus);
		if (normalized == null) {
			return null;
		}
		return normalizeScheduleStatus(normalized);
	}

	private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
		if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "startTime must be earlier than or equal to endTime");
		}
	}

	private String normalizeBlankToNull(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
