package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.utils.JsonUtil;
import com.example.cae.task.application.assembler.TaskAssembler;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.domain.model.TaskStatusHistory;
import com.example.cae.task.domain.service.TaskDomainService;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.domain.service.TaskValidationDomainService;
import com.example.cae.task.infrastructure.client.SolverClient;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.storage.TaskFileStorageService;
import com.example.cae.task.infrastructure.support.TaskNoGenerator;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.request.StatusReportRequest;
import com.example.cae.task.interfaces.request.UpdateTaskRequest;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import com.example.cae.task.interfaces.response.TaskFileResponse;
import com.example.cae.task.interfaces.response.TaskSubmitResponse;
import com.example.cae.task.interfaces.response.TaskUpdateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class TaskLifecycleManager {
	private static final Logger log = LoggerFactory.getLogger(TaskLifecycleManager.class);
	private final TaskRepository taskRepository;
	private final TaskFileRepository taskFileRepository;
	private final TaskStatusHistoryRepository taskStatusHistoryRepository;
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
								TaskStatusHistoryRepository taskStatusHistoryRepository,
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
		this.taskStatusHistoryRepository = taskStatusHistoryRepository;
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

	public TaskUpdateResponse updateTask(Long taskId, UpdateTaskRequest request, Long userId) {
		Task task = loadAndCheckOwner(taskId, userId);
		taskValidationDomainService.checkTaskEditable(task);
		ensureTaskUpdateRequestValid(request);

		boolean paramsChanged = false;
		if (request.getTaskName() != null) {
			task.setTaskName(request.getTaskName());
		}
		if (request.getPriority() != null) {
			task.setPriority(request.getPriority());
		}
		if (request.getParams() != null) {
			paramsChanged = !Objects.equals(parseParamsJson(task.getParamsJson()), request.getParams());
			task.setParamsJson(JsonUtil.toJson(request.getParams()));
		}
		if (paramsChanged && TaskStatusEnum.VALIDATED.name().equals(task.getStatus())) {
			taskStatusDomainService.transfer(task, TaskStatusEnum.CREATED.name(), "task parameters updated, re-validation required", OperatorTypeEnum.USER.name(), userId);
		}
		taskRepository.update(task);
		return taskAssembler.toUpdateResponse(task);
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

	public void adjustPriority(Long taskId, Integer priority, Long adminUserId) {
		Task task = loadTask(taskId);
		if (task.isFinished()) {
			throw new BizException(ErrorCodeConstants.TASK_PRIORITY_UPDATE_NOT_ALLOWED, "finished task priority cannot be adjusted");
		}

		Integer oldPriority = task.getPriority() == null ? 0 : task.getPriority();
		Integer newPriority = priority == null ? 0 : priority;
		task.adjustPriority(newPriority);
		taskRepository.update(task);

		TaskStatusHistory history = new TaskStatusHistory();
		history.setTaskId(task.getId());
		history.setFromStatus(task.getStatus());
		history.setToStatus(task.getStatus());
		history.setChangeReason("priority adjusted: " + oldPriority + " -> " + newPriority);
		history.setOperatorType(OperatorTypeEnum.ADMIN.name());
		history.setOperatorId(adminUserId);
		taskStatusHistoryRepository.save(history);
	}

	public TaskSubmitResponse retryTask(Long taskId, String reason, Long adminUserId) {
		Task task = loadTask(taskId);
		if (!TaskStatusEnum.FAILED.name().equals(task.getStatus()) && !TaskStatusEnum.TIMEOUT.name().equals(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_RETRY_NOT_ALLOWED, "only failed or timeout tasks can be retried");
		}

		String effectiveReason = reason == null || reason.isBlank() ? "admin retried task" : reason;
		taskStatusDomainService.transfer(task, TaskStatusEnum.QUEUED.name(), effectiveReason, OperatorTypeEnum.ADMIN.name(), adminUserId);
		taskRepository.update(task);
		schedulerClient.notifyTaskSubmitted(task.getId());

		TaskSubmitResponse response = new TaskSubmitResponse();
		response.setTaskId(task.getId());
		response.setStatus(task.getStatus());
		return response;
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

	@Transactional
	public void reportStatus(Long taskId, StatusReportRequest request) {
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (request != null && request.getFromStatus() != null && !request.getFromStatus().isBlank() && !request.getFromStatus().equalsIgnoreCase(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_STATUS_MISMATCH, "task status mismatch");
		}
		String targetStatus = pickStatus(request);
		String reason = pickReason(request);
		String operatorType = request == null || request.getOperatorType() == null || request.getOperatorType().isBlank()
				? OperatorTypeEnum.NODE.name()
				: request.getOperatorType();
		if (shouldIgnoreReportedStatus(task, targetStatus)) {
			return;
		}
		taskStatusDomainService.transfer(task, targetStatus, reason, operatorType, null);
		taskRepository.update(task);
		if (TaskStatusEnum.RUNNING.name().equalsIgnoreCase(targetStatus)) {
			releaseReservationQuietly(task.getNodeId());
		}
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

	private boolean shouldIgnoreReportedStatus(Task task, String targetStatus) {
		if (task == null || targetStatus == null) {
			return false;
		}
		String currentStatus = task.getStatus();
		if (targetStatus.equalsIgnoreCase(currentStatus)) {
			return true;
		}
		return task.isFinished();
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
		Task task = loadTask(taskId);
		if (!task.isOwner(userId)) {
			throw new BizException(ErrorCodeConstants.FORBIDDEN, "no permission");
		}
		return task;
	}

	private Task loadTask(Long taskId) {
		return taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
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

	private void ensureTaskUpdateRequestValid(UpdateTaskRequest request) {
		if (request == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "update request is required");
		}
		if (request.getTaskName() == null && request.getPriority() == null && request.getParams() == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "at least one editable field is required");
		}
		if (request.getTaskName() != null && request.getTaskName().isBlank()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "taskName cannot be blank");
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseParamsJson(String paramsJson) {
		if (paramsJson == null || paramsJson.isBlank()) {
			return null;
		}
		Object parsed = JsonUtil.fromJson(paramsJson, Map.class);
		if (parsed instanceof Map<?, ?> map) {
			return (Map<String, Object>) map;
		}
		return null;
	}

	private String extractSuffix(String fileName) {
		int idx = fileName.lastIndexOf('.');
		return idx < 0 ? "" : fileName.substring(idx + 1);
	}

	private void releaseReservationQuietly(Long nodeId) {
		if (nodeId == null) {
			return;
		}
		try {
			schedulerClient.releaseNodeReservation(nodeId);
		} catch (Exception ex) {
			log.warn("failed to release node reservation, nodeId={}", nodeId, ex);
		}
	}
}
