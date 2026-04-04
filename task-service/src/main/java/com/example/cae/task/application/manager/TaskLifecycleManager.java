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
import com.example.cae.task.infrastructure.client.SolverClient;
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

import java.util.List;
import java.util.Locale;

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
	private final SolverClient solverClient;
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
								SolverClient solverClient,
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
		this.solverClient = solverClient;
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
		UploadConstraint constraint = resolveUploadConstraint(task);
		validateArchiveUpload(task, file, constraint);
		TaskFile existingArchive = replaceExistingArchiveIfNeeded(task, constraint, userId);
		TaskFile taskFile = taskFileStorageService.saveInputFile(taskId, file, constraint.fileKey(), constraint.fileRole());
		taskFile.setArchiveFlag(1);
		if (existingArchive == null) {
			taskFileRepository.save(taskFile);
		} else {
			existingArchive.setFileRole(taskFile.getFileRole());
			existingArchive.setFileKey(taskFile.getFileKey());
			existingArchive.setOriginName(taskFile.getOriginName());
			existingArchive.setStoragePath(taskFile.getStoragePath());
			existingArchive.setUnpackDir(null);
			existingArchive.setRelativePath(null);
			existingArchive.setArchiveFlag(taskFile.getArchiveFlag());
			existingArchive.setFileSize(taskFile.getFileSize());
			existingArchive.setFileSuffix(taskFile.getFileSuffix());
			existingArchive.setChecksum(taskFile.getChecksum());
			taskFileRepository.update(existingArchive);
			taskFile = existingArchive;
		}
		return toTaskFileResponse(taskFile);
	}

	private UploadConstraint resolveUploadConstraint(Task task) {
		SolverClient.UploadSpecMeta uploadSpecMeta = solverClient.getUploadSpecMeta(task.getProfileId());
		String uploadMode = uploadSpecMeta == null || uploadSpecMeta.getUploadMode() == null
				? "ZIP_ONLY"
				: uploadSpecMeta.getUploadMode();
		if (!"ZIP_ONLY".equalsIgnoreCase(uploadMode)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "unsupported uploadMode: " + uploadMode);
		}
		String archiveFileKey = uploadSpecMeta == null || uploadSpecMeta.getArchiveFileKey() == null || uploadSpecMeta.getArchiveFileKey().isBlank()
				? "input_archive"
				: uploadSpecMeta.getArchiveFileKey();
		Integer maxSizeMb = uploadSpecMeta == null || uploadSpecMeta.getMaxSizeMb() == null ? 2048 : uploadSpecMeta.getMaxSizeMb();
		List<String> allowSuffix = uploadSpecMeta == null || uploadSpecMeta.getAllowSuffix() == null || uploadSpecMeta.getAllowSuffix().isEmpty()
				? List.of("zip")
				: uploadSpecMeta.getAllowSuffix();
		return new UploadConstraint(uploadMode, archiveFileKey, "ARCHIVE", maxSizeMb, allowSuffix);
	}

	private record UploadConstraint(String uploadMode, String fileKey, String fileRole, Integer maxSizeMb, List<String> allowSuffix) {
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

	public void discardTask(Long taskId, Long userId, String reason) {
		Task task = loadAndCheckOwner(taskId, userId);
		if (!taskDomainService.canDiscard(task)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "task cannot be discarded in current status");
		}
		discardTaskInternal(task, reason);
	}

	public int cleanStaleUnsubmittedTasks() {
		List<Task> staleTasks = taskRepository.listStaleUnsubmittedTasks(java.time.LocalDateTime.now().minusHours(24), 200);
		for (Task staleTask : staleTasks) {
			discardTaskInternal(staleTask, "stale unsubmitted task cleanup");
		}
		return staleTasks.size();
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

	private Task loadAndCheckOwner(Long taskId, Long userId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (!task.isOwner(userId)) {
			throw new BizException(ErrorCodeConstants.FORBIDDEN, "no permission");
		}
		return task;
	}

	private void validateArchiveUpload(Task task, MultipartFile file, UploadConstraint constraint) {
		if (file == null || file.isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "file is required");
		}
		String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
		String suffix = extractSuffix(originalName).toLowerCase(Locale.ROOT);
		boolean suffixAllowed = constraint.allowSuffix().stream().map(v -> v.toLowerCase(Locale.ROOT)).anyMatch(v -> v.equals(suffix));
		if (!suffixAllowed) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "only zip archive is supported");
		}
		long maxBytes = (long) constraint.maxSizeMb() * 1024 * 1024;
		if (file.getSize() > maxBytes) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "archive size exceeds limit");
		}
	}

	private TaskFile replaceExistingArchiveIfNeeded(Task task, UploadConstraint constraint, Long userId) {
		TaskFile existingArchive = taskFileRepository.listByTaskId(task.getId()).stream()
				.filter(existing -> constraint.fileRole().equalsIgnoreCase(existing.getFileRole())
						&& constraint.fileKey().equalsIgnoreCase(existing.getFileKey()))
				.findFirst()
				.orElse(null);
		if (existingArchive == null) {
			return null;
		}
		taskFileStorageService.deleteTaskArtifacts(task.getId());
		deleteOtherTaskFiles(task.getId(), existingArchive.getId());
		if (TaskStatusEnum.VALIDATED.name().equals(task.getStatus())) {
			taskStatusDomainService.transfer(task, TaskStatusEnum.CREATED.name(), "input archive replaced, re-validation required", OperatorTypeEnum.USER.name(), userId);
			taskRepository.update(task);
		}
		return existingArchive;
	}

	private void deleteOtherTaskFiles(Long taskId, Long keepId) {
		for (TaskFile taskFile : taskFileRepository.listByTaskId(taskId)) {
			if (keepId != null && keepId.equals(taskFile.getId())) {
				continue;
			}
			taskFileRepository.deleteById(taskFile.getId());
		}
	}

	private void discardTaskInternal(Task task, String reason) {
		deleteOtherTaskFiles(task.getId(), null);
		taskFileStorageService.deleteTaskArtifacts(task.getId());
		task.setDeletedFlag(1);
		taskRepository.update(task);
	}

	private String extractSuffix(String fileName) {
		int idx = fileName.lastIndexOf('.');
		return idx < 0 ? "" : fileName.substring(idx + 1);
	}
}
