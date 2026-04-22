package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskFileDTO;
import com.example.cae.common.dto.TaskScheduleClaimDTO;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.enums.FailTypeEnum;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.utils.JsonUtil;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.client.SolverClient;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TaskDispatchManager {
	private static final Logger log = LoggerFactory.getLogger(TaskDispatchManager.class);
	private static final Set<String> NODE_OFFLINE_AFFECTED_STATUSES = Set.of(
			TaskStatusEnum.SCHEDULED.name(),
			TaskStatusEnum.DISPATCHED.name(),
			TaskStatusEnum.RUNNING.name()
	);

	private final TaskRepository taskRepository;
	private final TaskFileRepository taskFileRepository;
	private final TaskStatusDomainService taskStatusDomainService;
	private final SchedulerClient schedulerClient;
	private final SolverClient solverClient;
	private final TaskStoragePathSupport taskStoragePathSupport;

	public TaskDispatchManager(TaskRepository taskRepository,
						   TaskFileRepository taskFileRepository,
						   TaskStatusDomainService taskStatusDomainService,
						   SchedulerClient schedulerClient,
						   SolverClient solverClient,
						   TaskStoragePathSupport taskStoragePathSupport) {
		this.taskRepository = taskRepository;
		this.taskFileRepository = taskFileRepository;
		this.taskStatusDomainService = taskStatusDomainService;
		this.schedulerClient = schedulerClient;
		this.solverClient = solverClient;
		this.taskStoragePathSupport = taskStoragePathSupport;
	}

	public List<TaskDTO> listQueuedTasks() {
		return taskRepository.listByStatus(TaskStatusEnum.QUEUED.name()).stream().map(this::toTaskDTO).toList();
	}

	public List<TaskDTO> listQueuedTasks(Integer limit) {
		return taskRepository.listByStatus(TaskStatusEnum.QUEUED.name()).stream()
				.limit(limit == null || limit < 1 ? Long.MAX_VALUE : limit.longValue())
				.map(this::toTaskDTO)
				.toList();
	}

	@Transactional
	public TaskScheduleClaimDTO markScheduled(Long taskId, Long nodeId) {
		if (nodeId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "nodeId is required");
		}
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (!TaskStatusEnum.QUEUED.name().equals(task.getStatus())) {
			if (Set.of(TaskStatusEnum.SCHEDULED.name(), TaskStatusEnum.DISPATCHED.name(), TaskStatusEnum.RUNNING.name(),
					TaskStatusEnum.SUCCESS.name(), TaskStatusEnum.FAILED.name(), TaskStatusEnum.CANCELED.name(), TaskStatusEnum.TIMEOUT.name())
					.contains(task.getStatus())) {
				return buildScheduleClaimResult(Boolean.FALSE, task, nodeId);
			}
			throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status for scheduling: " + task.getStatus());
		}
		task.bindNode(nodeId);
		taskStatusDomainService.transfer(task, TaskStatusEnum.SCHEDULED.name(), "scheduler selected node", OperatorTypeEnum.SYSTEM.name(), null);
		taskRepository.update(task);
		return buildScheduleClaimResult(Boolean.TRUE, task, nodeId);
	}

	@Transactional
	public void markDispatched(Long taskId, Long nodeId) {
		if (nodeId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "nodeId is required");
		}
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (!TaskStatusEnum.SCHEDULED.name().equals(task.getStatus())) {
			if (Set.of(TaskStatusEnum.DISPATCHED.name(), TaskStatusEnum.RUNNING.name(),
					TaskStatusEnum.SUCCESS.name(), TaskStatusEnum.FAILED.name(), TaskStatusEnum.CANCELED.name(), TaskStatusEnum.TIMEOUT.name())
					.contains(task.getStatus())) {
				ensureTaskBoundToNode(task, nodeId);
				return;
			}
			throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status for dispatch confirm: " + task.getStatus());
		}
		ensureTaskBoundToNode(task, nodeId);
		taskStatusDomainService.transfer(task, TaskStatusEnum.DISPATCHED.name(), "task dispatched", OperatorTypeEnum.SYSTEM.name(), null);
		taskRepository.update(task);
	}

	@Transactional
	public void markFailed(Long taskId, Long nodeId, String failType, String reason, Boolean recoverable) {
		if (nodeId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "nodeId is required");
		}
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (!Set.of(TaskStatusEnum.SCHEDULED.name(), TaskStatusEnum.DISPATCHED.name()).contains(task.getStatus())) {
			if (Boolean.TRUE.equals(recoverable) && TaskStatusEnum.QUEUED.name().equals(task.getStatus())) {
				return;
			}
			if (!Boolean.TRUE.equals(recoverable) && TaskStatusEnum.FAILED.name().equals(task.getStatus())) {
				return;
			}
			throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status for dispatch failure: " + task.getStatus());
		}
		ensureTaskBoundToNode(task, nodeId);
		task.setFailType(failType);
		task.setFailMessage(reason);
		taskStatusDomainService.transfer(task,
				Boolean.TRUE.equals(recoverable) ? TaskStatusEnum.QUEUED.name() : TaskStatusEnum.FAILED.name(),
				reason,
				OperatorTypeEnum.SYSTEM.name(),
				null);
		taskRepository.update(task);
	}

	private void ensureTaskBoundToNode(Task task, Long nodeId) {
		if (task == null || task.getNodeId() == null) {
			throw new BizException(ErrorCodeConstants.TASK_NOT_BOUND_TO_NODE, "task is not bound to node");
		}
		if (!task.getNodeId().equals(nodeId)) {
			throw new BizException(ErrorCodeConstants.REPORTED_NODE_MISMATCH, "nodeId does not match task bound node");
		}
	}

	private TaskScheduleClaimDTO buildScheduleClaimResult(Boolean claimed, Task task, Long requestedNodeId) {
		TaskScheduleClaimDTO result = new TaskScheduleClaimDTO();
		result.setClaimed(claimed);
		result.setTaskId(task == null ? null : task.getId());
		result.setNodeId(task != null && task.getNodeId() != null ? task.getNodeId() : requestedNodeId);
		result.setStatus(task == null ? null : task.getStatus());
		return result;
	}

	public String getTaskStatus(Long taskId) {
		return taskRepository.findById(taskId)
				.map(Task::getStatus)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
	}

	@Transactional
	public int markNodeOfflineTasksFailed(Long nodeId, String reason) {
		if (nodeId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "nodeId is required");
		}
		String effectiveReason = reason == null || reason.isBlank()
				? "node offline, task terminated by scheduler"
				: reason;
		List<Task> affectedTasks = taskRepository.listByNodeIdAndStatuses(nodeId, List.copyOf(NODE_OFFLINE_AFFECTED_STATUSES));
		for (Task task : affectedTasks) {
			Task lockedTask = taskRepository.findByIdForUpdate(task.getId()).orElse(null);
			if (lockedTask == null || !NODE_OFFLINE_AFFECTED_STATUSES.contains(lockedTask.getStatus())) {
				continue;
			}
			if (TaskStatusEnum.RUNNING.name().equals(lockedTask.getStatus())) {
				lockedTask.setFailType(FailTypeEnum.NODE_OFFLINE.name());
				lockedTask.setFailMessage(effectiveReason);
				taskStatusDomainService.transfer(lockedTask, TaskStatusEnum.FAILED.name(), effectiveReason, OperatorTypeEnum.SYSTEM.name(), null);
				taskRepository.update(lockedTask);
				continue;
			}
			if (Set.of(TaskStatusEnum.SCHEDULED.name(), TaskStatusEnum.DISPATCHED.name()).contains(lockedTask.getStatus())) {
				taskStatusDomainService.transfer(lockedTask, TaskStatusEnum.QUEUED.name(), effectiveReason, OperatorTypeEnum.SYSTEM.name(), null);
				taskRepository.update(lockedTask);
				releaseReservationQuietly(nodeId, lockedTask.getId());
			}
		}
		return affectedTasks.size();
	}

	private TaskDTO toTaskDTO(Task task) {
		TaskDTO dto = new TaskDTO();
		dto.setTaskId(task.getId());
		dto.setTaskNo(task.getTaskNo());
		dto.setTaskName(task.getTaskName());
		dto.setSolverId(task.getSolverId());
		dto.setProfileId(task.getProfileId());
		dto.setTaskType(task.getTaskType());
		dto.setPriority(task.getPriority());
		dto.setParamsJson(task.getParamsJson());
		dto.setParams(parseParams(task.getParamsJson()));
		dto.setInputFiles(loadInputFiles(task.getId()));
		enrichExecutionMeta(task, dto);
		dto.setNodeId(task.getNodeId());
		dto.setSubmitTime(task.getSubmitTime());
		return dto;
	}

	private void enrichExecutionMeta(Task task, TaskDTO dto) {
		try {
			SolverClient.SolverMeta solverMeta = solverClient.getSolverMeta(task.getSolverId());
			if (solverMeta != null) {
				if (solverMeta.getSolverCode() != null && !solverMeta.getSolverCode().isBlank()) {
					dto.setSolverCode(solverMeta.getSolverCode());
				}
				if (solverMeta.getExecMode() != null && !solverMeta.getExecMode().isBlank()) {
					dto.setSolverExecMode(solverMeta.getExecMode());
				}
				if (solverMeta.getExecPath() != null && !solverMeta.getExecPath().isBlank()) {
					dto.setSolverExecPath(solverMeta.getExecPath());
				}
			}
		} catch (Exception ignored) {
			// keep queued API resilient even if solver-service is temporarily unavailable.
		}

		try {
			SolverClient.ProfileExecutionMeta meta = solverClient.getProfileExecutionMeta(task.getProfileId());
			if (meta != null) {
				if (meta.getCommandTemplate() != null && !meta.getCommandTemplate().isBlank()) {
					dto.setCommandTemplate(meta.getCommandTemplate());
				}
				if (meta.getParserName() != null && !meta.getParserName().isBlank()) {
					dto.setParserName(meta.getParserName());
				}
				if (meta.getTimeoutSeconds() != null && meta.getTimeoutSeconds() > 0) {
					dto.setTimeoutSeconds(meta.getTimeoutSeconds());
				}
			}
		} catch (Exception ignored) {
			// keep queued API resilient even if solver-service is temporarily unavailable.
		}
	}

	private List<TaskFileDTO> loadInputFiles(Long taskId) {
		return taskFileRepository.listByTaskId(taskId).stream()
				.filter(file -> file.isInputFile() || file.isArchiveFile())
				.map(this::toTaskFileDTO)
				.toList();
	}

	private TaskFileDTO toTaskFileDTO(TaskFile file) {
		TaskFileDTO dto = new TaskFileDTO();
		dto.setTaskId(file.getTaskId());
		dto.setFileKey(file.getFileKey());
		dto.setOriginName(file.getOriginName());
		dto.setStoragePath(taskStoragePathSupport.toAbsoluteTaskPath(file.getStoragePath()));
		dto.setUnpackDir(taskStoragePathSupport.toAbsoluteTaskPath(file.getUnpackDir()));
		dto.setRelativePath(file.getRelativePath());
		dto.setArchiveFlag(file.getArchiveFlag());
		dto.setFileSize(file.getFileSize());
		return dto;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseParams(String paramsJson) {
		if (paramsJson == null || paramsJson.isBlank()) {
			return Map.of();
		}
		try {
			Object parsed = JsonUtil.fromJson(paramsJson, Map.class);
			if (parsed instanceof Map<?, ?> map) {
				return (Map<String, Object>) map;
			}
		} catch (Exception ignored) {
			// keep dispatch payload resilient when params are malformed.
		}
		return Map.of();
	}

	private void releaseReservationQuietly(Long nodeId, Long taskId) {
		if (nodeId == null || taskId == null) {
			return;
		}
		try {
			schedulerClient.releaseNodeReservation(nodeId, taskId);
		} catch (Exception ex) {
			log.warn("failed to release node reservation, nodeId={}, taskId={}", nodeId, taskId, ex);
		}
	}
}
