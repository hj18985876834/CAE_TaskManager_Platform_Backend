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
import com.example.cae.task.interfaces.response.TaskStatusHistoryResponse;
import com.example.cae.task.interfaces.response.TaskDashboardSummaryResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
		List<TaskListItemResponse> records = page.getRecords().stream().map(taskAssembler::toListItemResponse).map(this::enrichTaskListItem).toList();
		return PageResult.of(page.getTotal(), page.getPageNum(), page.getPageSize(), records);
	}

	public PageResult<TaskListItemResponse> pageAdminTasks(TaskListQueryRequest request) {
		request = taskQueryBuilder.sanitize(request);
		PageResult<Task> page = taskRepository.pageAdminTasks(request);
		List<TaskListItemResponse> records = page.getRecords().stream().map(taskAssembler::toListItemResponse).map(this::enrichTaskListItem).toList();
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
		String displayPath = taskStoragePathSupport.toDisplayTaskPath(file.getStoragePath());
		response.setStoragePath(displayPath);
		response.setRelativePath(displayPath);
		response.setFileSize(file.getFileSize());
		response.setFileSuffix(file.getFileSuffix());
		response.setChecksum(file.getChecksum());
		response.setCreatedAt(file.getCreatedAt());
		return response;
	}

	private TaskListItemResponse enrichTaskListItem(TaskListItemResponse response) {
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
		return response;
	}
}
