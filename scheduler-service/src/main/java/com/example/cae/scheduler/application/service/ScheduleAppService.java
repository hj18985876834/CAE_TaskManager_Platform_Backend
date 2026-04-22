package com.example.cae.scheduler.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.assembler.ScheduleAssembler;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeReservation;
import com.example.cae.scheduler.domain.model.ScheduleRecord;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeReservationRepository;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.domain.repository.ScheduleRecordRepository;
import com.example.cae.scheduler.domain.service.ScheduleDomainService;
import com.example.cae.scheduler.domain.strategy.ScheduleStrategy;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.infrastructure.client.SolverClient;
import com.example.cae.scheduler.interfaces.request.InternalScheduleRecordRequest;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;
import com.example.cae.scheduler.interfaces.response.ScheduleRecordResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScheduleAppService {
	private final ComputeNodeRepository computeNodeRepository;
	private final NodeReservationRepository nodeReservationRepository;
	private final NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
	private final ScheduleRecordRepository scheduleRecordRepository;
	private final ScheduleDomainService scheduleDomainService;
	private final ScheduleStrategy scheduleStrategy;
	private final NodeAgentClient nodeAgentClient;
	private final SolverClient solverClient;
	private final NodeAppService nodeAppService;

	public ScheduleAppService(ComputeNodeRepository computeNodeRepository,
							NodeReservationRepository nodeReservationRepository,
							NodeSolverCapabilityRepository nodeSolverCapabilityRepository,
							ScheduleRecordRepository scheduleRecordRepository,
							ScheduleDomainService scheduleDomainService,
							ScheduleStrategy scheduleStrategy,
							NodeAgentClient nodeAgentClient,
							SolverClient solverClient,
							NodeAppService nodeAppService) {
		this.computeNodeRepository = computeNodeRepository;
		this.nodeReservationRepository = nodeReservationRepository;
		this.nodeSolverCapabilityRepository = nodeSolverCapabilityRepository;
		this.scheduleRecordRepository = scheduleRecordRepository;
		this.scheduleDomainService = scheduleDomainService;
		this.scheduleStrategy = scheduleStrategy;
		this.nodeAgentClient = nodeAgentClient;
		this.solverClient = solverClient;
		this.nodeAppService = nodeAppService;
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

		ComputeNode lockedNode = computeNodeRepository.findByIdForUpdate(selected.getId())
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
		NodeReservation reservation = nodeReservationRepository.findByNodeIdAndTaskIdForUpdate(lockedNode.getId(), task.getTaskId()).orElse(null);
		if (reservation != null && reservation.isReserved()) {
			return lockedNode.getId();
		}
		if (!lockedNode.reserveSlot()) {
			throw new BizException(ErrorCodeConstants.NO_AVAILABLE_NODE, "no available node");
		}
		computeNodeRepository.update(lockedNode);
		if (reservation == null) {
			NodeReservation newReservation = new NodeReservation();
			newReservation.setNodeId(lockedNode.getId());
			newReservation.setTaskId(task.getTaskId());
			newReservation.markReserved();
			nodeReservationRepository.save(newReservation);
		} else {
			reservation.markReserved();
			nodeReservationRepository.update(reservation);
		}
		return lockedNode.getId();
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
	public void releaseNodeReservation(Long nodeId, Long taskId) {
		if (nodeId == null || taskId == null) {
			return;
		}
		nodeAppService.releaseReservation(nodeId, taskId);
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
				.map(this::enrichScheduleRecordResponse)
				.toList();
		return PageResult.of(page.getTotal(), pageNum, pageSize, records);
	}

	public void recordSchedule(InternalScheduleRecordRequest request) {
		if (request == null || request.getTaskId() == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid schedule record request");
		}
		ScheduleRecord record = ScheduleAssembler.newRecord(
				request.getTaskId(),
				request.getNodeId(),
				request.getStrategyName() == null || request.getStrategyName().isBlank() ? "FCFS_MIN_LOAD" : request.getStrategyName(),
				request.getScheduleStatus() == null || request.getScheduleStatus().isBlank() ? "UNKNOWN" : request.getScheduleStatus(),
				request.getScheduleMessage()
		);
		scheduleRecordRepository.save(record);
	}

	public List<ScheduleRecordResponse> listByTaskId(Long taskId) {
		if (taskId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "taskId is required");
		}
		return scheduleRecordRepository.listByTaskId(taskId).stream()
				.map(ScheduleAssembler::toResponse)
				.map(this::enrichScheduleRecordResponse)
				.toList();
	}

	private ScheduleRecordResponse enrichScheduleRecordResponse(ScheduleRecordResponse response) {
		if (response == null || response.getNodeId() == null) {
			return response;
		}
		computeNodeRepository.findById(response.getNodeId())
				.map(ComputeNode::getNodeName)
				.ifPresent(response::setNodeName);
		return response;
	}
}
