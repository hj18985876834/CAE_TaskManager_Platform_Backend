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
import com.example.cae.scheduler.domain.model.ScheduleRecord;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.domain.repository.ScheduleRecordRepository;
import com.example.cae.scheduler.domain.service.ScheduleDomainService;
import com.example.cae.scheduler.domain.strategy.ScheduleStrategy;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.infrastructure.client.SolverClient;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import com.example.cae.scheduler.interfaces.request.InternalScheduleRecordRequest;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;
import com.example.cae.scheduler.interfaces.response.NodeReservationActionResponse;
import com.example.cae.scheduler.interfaces.response.ScheduleRecordResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScheduleAppService {
	private static final Set<String> DISPATCH_FAILURE_RELEASE_TARGETS = Set.of("QUEUED", "FAILED");
	private final ComputeNodeRepository computeNodeRepository;
	private final NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
	private final ScheduleRecordRepository scheduleRecordRepository;
	private final ScheduleDomainService scheduleDomainService;
	private final ScheduleStrategy scheduleStrategy;
	private final NodeAgentClient nodeAgentClient;
	private final SolverClient solverClient;
	private final TaskClient taskClient;
	private final NodeCapacityManager nodeCapacityManager;

	public ScheduleAppService(ComputeNodeRepository computeNodeRepository,
							NodeSolverCapabilityRepository nodeSolverCapabilityRepository,
							ScheduleRecordRepository scheduleRecordRepository,
							ScheduleDomainService scheduleDomainService,
							ScheduleStrategy scheduleStrategy,
							NodeAgentClient nodeAgentClient,
							SolverClient solverClient,
							TaskClient taskClient,
							NodeCapacityManager nodeCapacityManager) {
		this.computeNodeRepository = computeNodeRepository;
		this.nodeSolverCapabilityRepository = nodeSolverCapabilityRepository;
		this.scheduleRecordRepository = scheduleRecordRepository;
		this.scheduleDomainService = scheduleDomainService;
		this.scheduleStrategy = scheduleStrategy;
		this.nodeAgentClient = nodeAgentClient;
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
		List<ComputeNode> availableNodes = scheduleDomainService.filterAvailableNodes(
				onlineNodes,
				task.getSolverId(),
				nodeSolverCapabilityRepository.listBySolverId(task.getSolverId())
		);

		ComputeNode selected = scheduleStrategy.selectNode(task, availableNodes);
		if (selected == null) {
			throw new BizException(ErrorCodeConstants.NO_AVAILABLE_NODE, "no available node");
		}

		NodeReservationActionResponse reservation = nodeCapacityManager.reserve(selected.getId(), task.getTaskId());
		return reservation.getNodeId();
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
	}

	public void confirmScheduleSuccess(Long taskId, Long nodeId, String scheduleMessage) {
		ScheduleRecord success = ScheduleAssembler.newRecord(
				taskId,
				nodeId,
				"FCFS_MIN_LOAD",
				"SUCCESS",
				scheduleMessage == null || scheduleMessage.isBlank() ? "dispatch success" : scheduleMessage
		);
		scheduleRecordRepository.save(success);
	}

	public void recordScheduleFailure(Long taskId, Long nodeId, String scheduleMessage) {
		ScheduleRecord failure = ScheduleAssembler.newRecord(
				taskId,
				nodeId,
				"FCFS_MIN_LOAD",
				"FAILED",
				scheduleMessage == null || scheduleMessage.isBlank() ? "dispatch failed" : scheduleMessage
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

	public void cancelTaskOnNode(Long nodeId, Long taskId, String reason) {
		if (nodeId == null || taskId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "nodeId and taskId are required");
		}
		nodeAgentClient.cancelTask(nodeId, taskId, reason);
	}

	public PageResult<ScheduleRecordResponse> pageRecords(SchedulePageQueryRequest request) {
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		int pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
		long offset = (long) (pageNum - 1) * pageSize;

		PageResult<ScheduleRecord> page = scheduleRecordRepository.page(request, offset, pageSize);
		List<ScheduleRecordResponse> records = page.getRecords().stream()
				.map(ScheduleAssembler::toResponse)
				.toList();
		enrichScheduleRecordResponses(records);
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
				request.getScheduleMessage()
		);
		scheduleRecordRepository.save(record);
	}

	public List<ScheduleRecordResponse> listByTaskId(Long taskId) {
		if (taskId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "taskId is required");
		}
		List<ScheduleRecordResponse> records = scheduleRecordRepository.listByTaskId(taskId).stream()
				.map(ScheduleAssembler::toResponse)
				.toList();
		enrichScheduleRecordResponses(records);
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
		return taskClient.getTaskBasics(taskIds);
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
}
