package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.application.assembler.TaskAssembler;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.domain.service.TaskDomainService;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.domain.service.TaskValidationDomainService;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.storage.TaskFileStorageService;
import com.example.cae.task.infrastructure.support.TaskNoGenerator;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.request.StatusReportRequest;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import com.example.cae.task.interfaces.response.TaskFileResponse;
import com.example.cae.task.interfaces.response.TaskSubmitResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TaskLifecycleManager {
	private final TaskRepository taskRepository;
	private final TaskFileRepository taskFileRepository;
	private final TaskDomainService taskDomainService;
	private final TaskStatusDomainService taskStatusDomainService;
	private final TaskValidationDomainService taskValidationDomainService;
	private final TaskFileStorageService taskFileStorageService;
	private final TaskAssembler taskAssembler;
	private final TaskNoGenerator taskNoGenerator;
	private final SchedulerClient schedulerClient;
	private final TaskStoragePathSupport taskStoragePathSupport;

	public TaskLifecycleManager(TaskRepository taskRepository,
								TaskFileRepository taskFileRepository,
								TaskDomainService taskDomainService,
								TaskStatusDomainService taskStatusDomainService,
								TaskValidationDomainService taskValidationDomainService,
								TaskFileStorageService taskFileStorageService,
								TaskAssembler taskAssembler,
								TaskNoGenerator taskNoGenerator,
								SchedulerClient schedulerClient,
								TaskStoragePathSupport taskStoragePathSupport) {
		this.taskRepository = taskRepository;
		this.taskFileRepository = taskFileRepository;
		this.taskDomainService = taskDomainService;
		this.taskStatusDomainService = taskStatusDomainService;
		this.taskValidationDomainService = taskValidationDomainService;
		this.taskFileStorageService = taskFileStorageService;
		this.taskAssembler = taskAssembler;
		this.taskNoGenerator = taskNoGenerator;
		this.schedulerClient = schedulerClient;
		this.taskStoragePathSupport = taskStoragePathSupport;
	}

	public TaskCreateResponse createTask(CreateTaskRequest request, Long userId) {
		Task task = taskAssembler.toTask(request, userId);
		task.setTaskNo(taskNoGenerator.generateTaskNo());
		task.setStatus(TaskStatusEnum.CREATED.name());
		taskRepository.save(task);
		taskStatusDomainService.recordInitialStatus(task, "task created", OperatorTypeEnum.USER.name(), userId);
		return taskAssembler.toCreateResponse(task);
	}

	public TaskFileResponse uploadTaskFile(Long taskId, MultipartFile file, String fileKey, String fileRole, Long userId) {
		Task task = loadAndCheckOwner(taskId, userId);
		taskValidationDomainService.checkTaskEditable(task);
		TaskFile taskFile = taskFileStorageService.saveInputFile(taskId, file, fileKey, fileRole);
		taskFileRepository.save(taskFile);
		return toTaskFileResponse(taskFile);
	}

	public TaskSubmitResponse submitTask(Long taskId, Long userId) {
		Task task = loadAndCheckOwner(taskId, userId);
		taskValidationDomainService.checkTaskCanSubmit(task);
		taskStatusDomainService.transfer(task, TaskStatusEnum.QUEUED.name(), "task submitted", OperatorTypeEnum.USER.name(), userId);
		taskRepository.update(task);
		schedulerClient.notifyTaskSubmitted(taskId);
		TaskSubmitResponse response = new TaskSubmitResponse();
		response.setTaskId(taskId);
		response.setStatus(task.getStatus());
		return response;
	}

	public void cancelTask(Long taskId, Long userId, String reason) {
		Task task = loadAndCheckOwner(taskId, userId);
		if (!taskDomainService.canCancel(task)) {
			throw new BizException(ErrorCodeConstants.TASK_CANCEL_NOT_ALLOWED, "task cannot be canceled in current status");
		}
		if (TaskStatusEnum.RUNNING.name().equals(task.getStatus())) {
			if (task.getNodeId() == null) {
				throw new BizException(ErrorCodeConstants.TASK_NOT_BOUND_TO_NODE, "running task is not bound to node");
			}
			schedulerClient.cancelTaskOnNode(task.getNodeId(), taskId, reason);
			return;
		}
		taskStatusDomainService.transfer(task, TaskStatusEnum.CANCELED.name(), reason, OperatorTypeEnum.USER.name(), userId);
		taskRepository.update(task);
	}

	public void markScheduled(Long taskId, Long nodeId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		task.bindNode(nodeId);
		taskStatusDomainService.transfer(task, TaskStatusEnum.SCHEDULED.name(), "scheduler selected node", OperatorTypeEnum.SYSTEM.name(), null);
		taskRepository.update(task);
	}

	public void markDispatched(Long taskId, Long nodeId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		task.bindNode(nodeId);
		taskStatusDomainService.transfer(task, TaskStatusEnum.DISPATCHED.name(), "task dispatched", OperatorTypeEnum.SYSTEM.name(), null);
		taskRepository.update(task);
	}

	public void reportStatus(Long taskId, StatusReportRequest request) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (request != null && request.getFromStatus() != null && !request.getFromStatus().isBlank() && !request.getFromStatus().equalsIgnoreCase(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_STATUS_MISMATCH, "task status mismatch");
		}
		String targetStatus = pickStatus(request);
		String reason = pickReason(request);
		String operatorType = request == null || request.getOperatorType() == null || request.getOperatorType().isBlank()
				? OperatorTypeEnum.NODE.name()
				: request.getOperatorType();
		taskStatusDomainService.transfer(task, targetStatus, reason, operatorType, null);
		taskRepository.update(task);
	}

	private String pickStatus(StatusReportRequest request) {
		if (request == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "status report request is required");
		}
		if (request.getToStatus() != null && !request.getToStatus().isBlank()) {
			return request.getToStatus();
		}
		if (request.getStatus() != null && !request.getStatus().isBlank()) {
			return request.getStatus();
		}
		throw new BizException(ErrorCodeConstants.BAD_REQUEST, "status is required");
	}

	private String pickReason(StatusReportRequest request) {
		if (request == null) {
			return null;
		}
		if (request.getChangeReason() != null && !request.getChangeReason().isBlank()) {
			return request.getChangeReason();
		}
		return request.getReason();
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

	private Task loadAndCheckOwner(Long taskId, Long userId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (!task.isOwner(userId)) {
			throw new BizException(ErrorCodeConstants.FORBIDDEN, "no permission");
		}
		return task;
	}
}
