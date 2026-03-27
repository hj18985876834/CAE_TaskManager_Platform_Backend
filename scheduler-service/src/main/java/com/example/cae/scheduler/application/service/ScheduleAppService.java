package com.example.cae.scheduler.application.service;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.assembler.ScheduleAssembler;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.ScheduleRecord;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.domain.repository.ScheduleRecordRepository;
import com.example.cae.scheduler.domain.service.ScheduleDomainService;
import com.example.cae.scheduler.domain.strategy.ScheduleStrategy;
import com.example.cae.scheduler.interfaces.request.InternalScheduleRecordRequest;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;
import com.example.cae.scheduler.interfaces.response.ScheduleRecordResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleAppService {
	private final ComputeNodeRepository computeNodeRepository;
	private final NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
	private final ScheduleRecordRepository scheduleRecordRepository;
	private final ScheduleDomainService scheduleDomainService;
	private final ScheduleStrategy scheduleStrategy;

	public ScheduleAppService(ComputeNodeRepository computeNodeRepository,
							NodeSolverCapabilityRepository nodeSolverCapabilityRepository,
							ScheduleRecordRepository scheduleRecordRepository,
							ScheduleDomainService scheduleDomainService,
							ScheduleStrategy scheduleStrategy) {
		this.computeNodeRepository = computeNodeRepository;
		this.nodeSolverCapabilityRepository = nodeSolverCapabilityRepository;
		this.scheduleRecordRepository = scheduleRecordRepository;
		this.scheduleDomainService = scheduleDomainService;
		this.scheduleStrategy = scheduleStrategy;
	}

	public Long scheduleTask(TaskDTO task) {
		if (task == null || task.getTaskId() == null || task.getSolverId() == null) {
			throw new BizException(400, "invalid task for scheduling");
		}

		List<ComputeNode> onlineNodes = computeNodeRepository.listByStatus("ONLINE");
		List<ComputeNode> availableNodes = scheduleDomainService.filterAvailableNodes(
				onlineNodes,
				task.getSolverId(),
				nodeSolverCapabilityRepository.listBySolverId(task.getSolverId())
		);

		ComputeNode selected = scheduleStrategy.selectNode(task, availableNodes);
		if (selected == null) {
			ScheduleRecord failure = ScheduleAssembler.newRecord(task.getTaskId(), null, "FCFS_LEAST_LOAD", "FAILED", "no available node");
			scheduleRecordRepository.save(failure);
			throw new BizException(409, "no available node");
		}

		selected.setRunningCount((selected.getRunningCount() == null ? 0 : selected.getRunningCount()) + 1);
		computeNodeRepository.update(selected);

		ScheduleRecord success = ScheduleAssembler.newRecord(task.getTaskId(), selected.getId(), "FCFS_LEAST_LOAD", "SUCCESS", "node selected");
		scheduleRecordRepository.save(success);
		return selected.getId();
	}

	public PageResult<ScheduleRecordResponse> pageRecords(SchedulePageQueryRequest request) {
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		int pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
		long offset = (long) (pageNum - 1) * pageSize;

		PageResult<ScheduleRecord> page = scheduleRecordRepository.page(request, offset, pageSize);
		List<ScheduleRecordResponse> records = page.getRecords().stream().map(ScheduleAssembler::toResponse).toList();
		return PageResult.of(page.getTotal(), pageNum, pageSize, records);
	}

	public void recordSchedule(InternalScheduleRecordRequest request) {
		if (request == null || request.getTaskId() == null) {
			throw new BizException(400, "invalid schedule record request");
		}
		ScheduleRecord record = ScheduleAssembler.newRecord(
				request.getTaskId(),
				request.getNodeId(),
				request.getStrategyName() == null || request.getStrategyName().isBlank() ? "FCFS_LEAST_LOAD" : request.getStrategyName(),
				request.getScheduleStatus() == null || request.getScheduleStatus().isBlank() ? "UNKNOWN" : request.getScheduleStatus(),
				request.getScheduleMessage()
		);
		scheduleRecordRepository.save(record);
	}

	public List<ScheduleRecordResponse> listByTaskId(Long taskId) {
		if (taskId == null) {
			throw new BizException(400, "taskId is required");
		}
		return scheduleRecordRepository.listByTaskId(taskId).stream().map(ScheduleAssembler::toResponse).toList();
	}
}

