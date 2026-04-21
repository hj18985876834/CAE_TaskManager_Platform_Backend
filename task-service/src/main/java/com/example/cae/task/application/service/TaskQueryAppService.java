package com.example.cae.task.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.response.PageResult;
import com.example.cae.task.application.assembler.TaskAssembler;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.domain.model.TaskStatusHistory;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.client.SolverClient;
import com.example.cae.task.infrastructure.support.TaskQueryBuilder;
import com.example.cae.task.infrastructure.support.TaskPermissionChecker;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.request.TaskListQueryRequest;
import com.example.cae.task.interfaces.response.TaskDetailResponse;
import com.example.cae.task.interfaces.response.TaskFileResponse;
import com.example.cae.task.interfaces.response.TaskListItemResponse;
import com.example.cae.task.interfaces.response.TaskScheduleRecordResponse;
import com.example.cae.task.interfaces.response.TaskStatusHistoryResponse;
import com.example.cae.task.interfaces.response.TaskDashboardSummaryResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskQueryAppService {
	private final TaskRepository taskRepository;
	private final TaskFileRepository taskFileRepository;
	private final TaskStatusHistoryRepository taskStatusHistoryRepository;
	private final TaskAssembler taskAssembler;
	private final TaskPermissionChecker taskPermissionChecker;
	private final TaskQueryBuilder taskQueryBuilder;
	private final SolverClient solverClient;
	private final SchedulerClient schedulerClient;
	private final TaskStoragePathSupport taskStoragePathSupport;

	public TaskQueryAppService(TaskRepository taskRepository,
							   TaskFileRepository taskFileRepository,
							   TaskStatusHistoryRepository taskStatusHistoryRepository,
							   TaskAssembler taskAssembler,
							   TaskPermissionChecker taskPermissionChecker,
							   TaskQueryBuilder taskQueryBuilder,
							   SolverClient solverClient,
							   SchedulerClient schedulerClient,
							   TaskStoragePathSupport taskStoragePathSupport) {
		this.taskRepository = taskRepository;
		this.taskFileRepository = taskFileRepository;
		this.taskStatusHistoryRepository = taskStatusHistoryRepository;
		this.taskAssembler = taskAssembler;
		this.taskPermissionChecker = taskPermissionChecker;
		this.taskQueryBuilder = taskQueryBuilder;
		this.solverClient = solverClient;
		this.schedulerClient = schedulerClient;
		this.taskStoragePathSupport = taskStoragePathSupport;
	}

	public PageResult<TaskListItemResponse> pageMyTasks(TaskListQueryRequest request, Long userId) {
		request = taskQueryBuilder.sanitize(request);
		PageResult<Task> page = taskRepository.pageMyTasks(request, userId);
		QueueReasonContext queueReasonContext = buildQueueReasonContext();
		List<TaskListItemResponse> records = page.getRecords().stream()
				.map(task -> enrichTaskListItem(taskAssembler.toListItemResponse(task), queueReasonContext))
				.toList();
		return PageResult.of(page.getTotal(), page.getPageNum(), page.getPageSize(), records);
	}

	public PageResult<TaskListItemResponse> pageAdminTasks(TaskListQueryRequest request) {
		request = taskQueryBuilder.sanitize(request);
		PageResult<Task> page = taskRepository.pageAdminTasks(request);
		QueueReasonContext queueReasonContext = buildQueueReasonContext();
		List<TaskListItemResponse> records = page.getRecords().stream()
				.map(task -> enrichTaskListItem(taskAssembler.toListItemResponse(task), queueReasonContext))
				.toList();
		return PageResult.of(page.getTotal(), page.getPageNum(), page.getPageSize(), records);
	}

	public TaskDetailResponse getTaskDetail(Long taskId, Long userId, String roleCode) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		return enrichTaskDetail(taskAssembler.toDetailResponse(task));
	}

	public List<TaskStatusHistoryResponse> getTaskStatusHistory(Long taskId, Long userId, String roleCode) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		return taskStatusHistoryRepository.listByTaskId(taskId).stream().map(this::toStatusHistoryResponse).toList();
	}

	public List<TaskFileResponse> getTaskFiles(Long taskId, Long userId, String roleCode) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		return taskFileRepository.listByTaskId(taskId).stream().map(this::toTaskFileResponse).toList();
	}

	public TaskDashboardSummaryResponse getDashboardSummary() {
		long totalTaskCount = taskRepository.countAll();
		long runningTaskCount = taskRepository.countByStatus(TaskStatusEnum.RUNNING.name());
		long queuedTaskCount = taskRepository.countByStatus(TaskStatusEnum.QUEUED.name());
		long successTaskCount = taskRepository.countByStatus(TaskStatusEnum.SUCCESS.name());
		long finishedTaskCount = taskRepository.countFinished();

		SchedulerClient.NodeSummary nodeSummary;
		try {
			nodeSummary = schedulerClient.getOnlineNodeSummary();
		} catch (Exception ignored) {
			nodeSummary = SchedulerClient.NodeSummary.empty();
		}

		TaskDashboardSummaryResponse response = new TaskDashboardSummaryResponse();
		response.setTotalTaskCount(totalTaskCount);
		response.setRunningTaskCount(runningTaskCount);
		response.setQueuedTaskCount(queuedTaskCount);
		response.setSuccessRate(finishedTaskCount == 0
				? BigDecimal.ZERO
				: BigDecimal.valueOf(successTaskCount)
						.divide(BigDecimal.valueOf(finishedTaskCount), 4, RoundingMode.HALF_UP));
		response.setOnlineNodeCount(nodeSummary.getOnlineNodeCount());
		response.setAvgNodeLoad(nodeSummary.getAvgNodeLoad());
		return response;
	}

	private TaskStatusHistoryResponse toStatusHistoryResponse(TaskStatusHistory history) {
		TaskStatusHistoryResponse response = new TaskStatusHistoryResponse();
		response.setId(history.getId());
		response.setTaskId(history.getTaskId());
		response.setFromStatus(history.getFromStatus());
		response.setToStatus(history.getToStatus());
		response.setChangeReason(history.getChangeReason());
		response.setOperatorType(history.getOperatorType());
		response.setOperatorId(history.getOperatorId());
		response.setCreatedAt(history.getCreatedAt());
		return response;
	}

	private TaskFileResponse toTaskFileResponse(TaskFile file) {
		TaskFileResponse response = new TaskFileResponse();
		response.setId(file.getId());
		response.setTaskId(file.getTaskId());
		response.setFileRole(file.getFileRole());
		response.setFileKey(file.getFileKey());
		response.setOriginName(file.getOriginName());
		response.setStoragePath(taskStoragePathSupport.toDisplayTaskPath(file.getStoragePath()));
		response.setUnpackDir(file.getUnpackDir());
		response.setRelativePath(file.getRelativePath());
		response.setArchiveFlag(file.getArchiveFlag());
		response.setFileSize(file.getFileSize());
		response.setFileSuffix(file.getFileSuffix());
		response.setChecksum(file.getChecksum());
		response.setCreatedAt(file.getCreatedAt());
		return response;
	}

	private TaskListItemResponse enrichTaskListItem(TaskListItemResponse response, QueueReasonContext queueReasonContext) {
		try {
			response.setSolverName(solverClient.getSolverName(response.getSolverId()));
		} catch (Exception ignored) {
		}
		try {
			response.setProfileName(solverClient.getProfileName(response.getProfileId()));
		} catch (Exception ignored) {
		}
		try {
			response.setNodeName(schedulerClient.getNodeName(response.getNodeId()));
		} catch (Exception ignored) {
		}
		response.setQueueReason(resolveQueueReason(response.getTaskId(), response.getSolverId(), response.getStatus(), queueReasonContext));
		return response;
	}

	private TaskDetailResponse enrichTaskDetail(TaskDetailResponse response) {
		try {
			response.setSolverName(solverClient.getSolverName(response.getSolverId()));
		} catch (Exception ignored) {
		}
		try {
			response.setProfileName(solverClient.getProfileName(response.getProfileId()));
		} catch (Exception ignored) {
		}
		try {
			response.setNodeName(schedulerClient.getNodeName(response.getNodeId()));
		} catch (Exception ignored) {
		}
		response.setQueueReason(resolveQueueReason(response.getTaskId(), response.getSolverId(), response.getStatus(), buildQueueReasonContext()));
		response.setStatusHistory(taskStatusHistoryRepository.listByTaskId(response.getTaskId()).stream()
				.map(this::toStatusHistoryResponse)
				.toList());
		try {
			response.setScheduleRecords(schedulerClient.listTaskScheduleRecords(response.getTaskId()).stream()
					.map(this::toTaskScheduleRecordResponse)
					.toList());
		} catch (Exception ignored) {
			response.setScheduleRecords(List.of());
		}
		return response;
	}

	private TaskScheduleRecordResponse toTaskScheduleRecordResponse(SchedulerClient.ScheduleRecordItem item) {
		TaskScheduleRecordResponse response = new TaskScheduleRecordResponse();
		response.setId(item.getId());
		response.setTaskId(item.getTaskId());
		response.setNodeId(item.getNodeId());
		response.setNodeName(item.getNodeName());
		response.setStrategyName(item.getStrategyName());
		response.setScheduleStatus(item.getScheduleStatus());
		response.setScheduleMessage(item.getScheduleMessage());
		response.setCreatedAt(item.getCreatedAt());
		return response;
	}

	private String resolveQueueReason(Long taskId, Long solverId, String status, QueueReasonContext queueReasonContext) {
		if (!TaskStatusEnum.QUEUED.name().equalsIgnoreCase(status)) {
			return null;
		}
		if (queueReasonContext != null) {
			Integer queueOrder = queueReasonContext.queuedTaskOrder().get(taskId);
			if (queueOrder != null && queueOrder > 0) {
				return "前方仍有更高优先级或更早提交任务";
			}
		}
		try {
			QueueReasonContext effectiveContext = queueReasonContext == null ? buildQueueReasonContext() : queueReasonContext;
			SchedulerClient.QueueNodeSnapshot snapshot = effectiveContext.queueSnapshotCache().computeIfAbsent(
					solverId,
					key -> schedulerClient.getQueueNodeSnapshot(key)
			);
			int dispatchableNodeCount = snapshot.getDispatchableNodeCount() == null ? 0 : snapshot.getDispatchableNodeCount();
			if (dispatchableNodeCount <= 0) {
				int onlineEnabledCapableNodeCount = snapshot.getOnlineEnabledCapableNodeCount() == null
						? 0
						: snapshot.getOnlineEnabledCapableNodeCount();
				if (onlineEnabledCapableNodeCount <= 0) {
					return "暂无满足条件的可用节点";
				}
				return "候选节点当前满载，等待资源释放";
			}
			return "前方仍有更高优先级或更早提交任务";
		} catch (Exception ignored) {
			return "排队中，等待调度器处理";
		}
	}

	private QueueReasonContext buildQueueReasonContext() {
		List<Task> queuedTasks = taskRepository.listByStatus(TaskStatusEnum.QUEUED.name());
		Map<Long, Integer> queuedTaskOrder = new HashMap<>();
		for (int i = 0; i < queuedTasks.size(); i++) {
			queuedTaskOrder.put(queuedTasks.get(i).getId(), i);
		}
		return new QueueReasonContext(queuedTaskOrder, new HashMap<>());
	}

	private record QueueReasonContext(Map<Long, Integer> queuedTaskOrder,
									  Map<Long, SchedulerClient.QueueNodeSnapshot> queueSnapshotCache) {
	}
}
