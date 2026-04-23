package com.example.cae.task.application.service;

import com.example.cae.common.dto.TaskBasicDTO;
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
import com.example.cae.task.infrastructure.client.UserClient;
import com.example.cae.task.infrastructure.support.TaskQueryBuilder;
import com.example.cae.task.infrastructure.support.TaskPermissionChecker;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.request.AdminTaskListQueryRequest;
import com.example.cae.task.interfaces.request.MyTaskListQueryRequest;
import com.example.cae.task.interfaces.request.TaskListQueryRequest;
import com.example.cae.task.interfaces.response.AdminTaskListItemResponse;
import com.example.cae.task.interfaces.response.TaskDetailResponse;
import com.example.cae.task.interfaces.response.TaskInputFileResponse;
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
	private final UserClient userClient;
	private final TaskStoragePathSupport taskStoragePathSupport;

	public TaskQueryAppService(TaskRepository taskRepository,
							   TaskFileRepository taskFileRepository,
							   TaskStatusHistoryRepository taskStatusHistoryRepository,
							   TaskAssembler taskAssembler,
							   TaskPermissionChecker taskPermissionChecker,
							   TaskQueryBuilder taskQueryBuilder,
							   SolverClient solverClient,
							   SchedulerClient schedulerClient,
							   UserClient userClient,
							   TaskStoragePathSupport taskStoragePathSupport) {
		this.taskRepository = taskRepository;
		this.taskFileRepository = taskFileRepository;
		this.taskStatusHistoryRepository = taskStatusHistoryRepository;
		this.taskAssembler = taskAssembler;
		this.taskPermissionChecker = taskPermissionChecker;
		this.taskQueryBuilder = taskQueryBuilder;
		this.solverClient = solverClient;
		this.schedulerClient = schedulerClient;
		this.userClient = userClient;
		this.taskStoragePathSupport = taskStoragePathSupport;
	}

	public PageResult<TaskListItemResponse> pageMyTasks(MyTaskListQueryRequest request, Long userId) {
		TaskListQueryRequest query = taskQueryBuilder.sanitize(toMyTaskQuery(request));
		PageResult<Task> page = taskRepository.pageMyTasks(query, userId);
		QueueReasonContext queueReasonContext = buildQueueReasonContext();
		List<TaskListItemResponse> records = page.getRecords().stream()
				.map(task -> enrichTaskListItem(taskAssembler.toListItemResponse(task), queueReasonContext))
				.toList();
		return PageResult.of(page.getTotal(), page.getPageNum(), page.getPageSize(), records);
	}

	public PageResult<AdminTaskListItemResponse> pageAdminTasks(AdminTaskListQueryRequest request) {
		TaskListQueryRequest query = taskQueryBuilder.sanitize(toAdminTaskQuery(request));
		PageResult<Task> page = taskRepository.pageAdminTasks(query);
		QueueReasonContext queueReasonContext = buildQueueReasonContext();
		Map<Long, String> usernameMap = buildUsernameMap(page.getRecords());
		List<AdminTaskListItemResponse> records = page.getRecords().stream()
				.map(task -> enrichAdminTaskListItem(taskAssembler.toAdminListItemResponse(task), queueReasonContext, usernameMap))
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

	public List<TaskInputFileResponse> getTaskFiles(Long taskId, Long userId, String roleCode) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		return taskFileRepository.listByTaskId(taskId).stream().map(this::toTaskInputFileResponse).toList();
	}

	public List<TaskScheduleRecordResponse> getTaskScheduleRecords(Long taskId, Long userId, String roleCode) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		return schedulerClient.listTaskScheduleRecords(taskId).stream()
				.map(item -> toTaskScheduleRecordResponse(item, task.getTaskNo()))
				.toList();
	}

	public List<TaskBasicDTO> listTaskBasics(List<Long> taskIds) {
		return taskRepository.listByIds(taskIds).stream()
				.map(task -> {
					TaskBasicDTO dto = new TaskBasicDTO();
					dto.setTaskId(task.getId());
					dto.setTaskNo(task.getTaskNo());
					return dto;
				})
				.toList();
	}

	public TaskDashboardSummaryResponse getDashboardSummary() {
		long totalTaskCount = taskRepository.countAll();
		long runningTaskCount = taskRepository.countByStatus(TaskStatusEnum.RUNNING.name());
		long queuedTaskCount = taskRepository.countByStatus(TaskStatusEnum.QUEUED.name());
		long successTaskCount = taskRepository.countByStatus(TaskStatusEnum.SUCCESS.name());
		long finishedTaskCount = taskRepository.countFinished();
		SchedulerClient.NodeSummary nodeSummary = schedulerClient.getOnlineNodeSummary();

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
		response.setFromStatus(history.getFromStatus());
		response.setToStatus(history.getToStatus());
		response.setChangeReason(history.getChangeReason());
		response.setOperatorType(history.getOperatorType());
		response.setCreatedAt(history.getCreatedAt());
		return response;
	}

	private TaskInputFileResponse toTaskInputFileResponse(TaskFile file) {
		TaskInputFileResponse response = new TaskInputFileResponse();
		response.setFileId(file.getId());
		response.setFileRole(file.getFileRole());
		response.setFileKey(file.getFileKey());
		response.setOriginName(file.getOriginName());
		response.setStoragePath(taskStoragePathSupport.toDisplayTaskPath(file.getStoragePath()));
		response.setUnpackDir(taskStoragePathSupport.toDisplayTaskPath(file.getUnpackDir()));
		response.setFileSize(file.getFileSize());
		response.setFileSuffix(file.getFileSuffix());
		response.setCreatedAt(file.getCreatedAt());
		return response;
	}

	private TaskListItemResponse enrichTaskListItem(TaskListItemResponse response, QueueReasonContext queueReasonContext) {
		response.setSolverName(solverClient.getSolverName(response.getSolverId()));
		response.setProfileName(solverClient.getProfileName(response.getProfileId()));
		response.setNodeName(schedulerClient.getNodeName(response.getNodeId()));
		response.setQueueReason(resolveQueueReason(response.getTaskId(), response.getSolverId(), response.getStatus(), queueReasonContext));
		return response;
	}

	private AdminTaskListItemResponse enrichAdminTaskListItem(AdminTaskListItemResponse response,
															  QueueReasonContext queueReasonContext,
															  Map<Long, String> usernameMap) {
		enrichTaskListItem(response, queueReasonContext);
		response.setUsername(usernameMap.get(response.getUserId()));
		return response;
	}

	private TaskDetailResponse enrichTaskDetail(TaskDetailResponse response) {
		response.setSolverName(solverClient.getSolverName(response.getSolverId()));
		response.setProfileName(solverClient.getProfileName(response.getProfileId()));
		response.setNodeName(schedulerClient.getNodeName(response.getNodeId()));
		response.setQueueReason(resolveQueueReason(response.getTaskId(), response.getSolverId(), response.getStatus(), buildQueueReasonContext()));
		return response;
	}

	private TaskScheduleRecordResponse toTaskScheduleRecordResponse(SchedulerClient.ScheduleRecordItem item, String fallbackTaskNo) {
		TaskScheduleRecordResponse response = new TaskScheduleRecordResponse();
		response.setScheduleId(item.getScheduleId());
		response.setTaskId(item.getTaskId());
		response.setTaskNo(item.getTaskNo() == null || item.getTaskNo().isBlank() ? fallbackTaskNo : item.getTaskNo());
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
				return "前方仍有更高优先级或更早提交的任务，等待调度";
			}
		}
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
		return "排队中，等待调度器处理";
	}

	private QueueReasonContext buildQueueReasonContext() {
		List<Task> queuedTasks = taskRepository.listByStatus(TaskStatusEnum.QUEUED.name());
		Map<Long, Integer> queuedTaskOrder = new HashMap<>();
		for (int i = 0; i < queuedTasks.size(); i++) {
			queuedTaskOrder.put(queuedTasks.get(i).getId(), i);
		}
		return new QueueReasonContext(queuedTaskOrder, new HashMap<>());
	}

	private Map<Long, String> buildUsernameMap(List<Task> tasks) {
		Map<Long, String> usernameMap = new HashMap<>();
		if (tasks == null || tasks.isEmpty()) {
			return usernameMap;
		}
		for (Task task : tasks) {
			if (task == null || task.getUserId() == null || usernameMap.containsKey(task.getUserId())) {
				continue;
			}
			usernameMap.put(task.getUserId(), userClient.getUsername(task.getUserId()));
		}
		return usernameMap;
	}

	private record QueueReasonContext(Map<Long, Integer> queuedTaskOrder,
									  Map<Long, SchedulerClient.QueueNodeSnapshot> queueSnapshotCache) {
	}

	private TaskListQueryRequest toMyTaskQuery(MyTaskListQueryRequest request) {
		TaskListQueryRequest query = new TaskListQueryRequest();
		if (request == null) {
			return query;
		}
		query.setPageNum(request.getPageNum());
		query.setPageSize(request.getPageSize());
		query.setTaskName(request.getTaskName());
		query.setStatus(request.getStatus());
		query.setPriority(request.getPriority());
		query.setSolverId(request.getSolverId());
		query.setTaskType(request.getTaskType());
		query.setStartTime(request.getStartTime());
		query.setEndTime(request.getEndTime());
		return query;
	}

	private TaskListQueryRequest toAdminTaskQuery(AdminTaskListQueryRequest request) {
		TaskListQueryRequest query = new TaskListQueryRequest();
		if (request == null) {
			return query;
		}
		query.setPageNum(request.getPageNum());
		query.setPageSize(request.getPageSize());
		query.setTaskName(request.getTaskName());
		query.setStatus(request.getStatus());
		query.setPriority(request.getPriority());
		query.setSolverId(request.getSolverId());
		query.setTaskType(request.getTaskType());
		query.setUserId(request.getUserId());
		query.setNodeId(request.getNodeId());
		query.setFailType(request.getFailType());
		query.setStartTime(request.getStartTime());
		query.setEndTime(request.getEndTime());
		return query;
	}
}
