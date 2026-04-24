package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskStatusAckDTO;
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
import com.example.cae.task.domain.repository.TaskLogRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskResultFileRepository;
import com.example.cae.task.domain.repository.TaskResultSummaryRepository;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.domain.service.TaskValidationDomainService;
import com.example.cae.task.infrastructure.client.SolverClient;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.storage.TaskFileStorageService;
import com.example.cae.task.application.support.TaskParamSchemaValidator;
import com.example.cae.task.infrastructure.support.TaskNoGenerator;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.request.StatusReportRequest;
import com.example.cae.task.interfaces.request.UpdateTaskRequest;
import com.example.cae.task.interfaces.response.TaskActionResponse;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import com.example.cae.task.interfaces.response.TaskFileUploadResponse;
import com.example.cae.task.interfaces.response.TaskPriorityUpdateResponse;
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
	private final TaskLogRepository taskLogRepository;
	private final TaskResultSummaryRepository taskResultSummaryRepository;
	private final TaskResultFileRepository taskResultFileRepository;
	private final TaskDomainService taskDomainService;
	private final TaskStatusDomainService taskStatusDomainService;
	private final TaskValidationDomainService taskValidationDomainService;
	private final TaskValidationManager taskValidationManager;
	private final TaskFileStorageService taskFileStorageService;
	private final TaskParamSchemaValidator taskParamSchemaValidator;
	private final TaskAssembler taskAssembler;
	private final TaskNoGenerator taskNoGenerator;
	private final SchedulerClient schedulerClient;
	private final SolverClient solverClient;
	private final TaskStoragePathSupport taskStoragePathSupport;

	public TaskLifecycleManager(TaskRepository taskRepository,
								TaskFileRepository taskFileRepository,
								TaskStatusHistoryRepository taskStatusHistoryRepository,
								TaskLogRepository taskLogRepository,
								TaskResultSummaryRepository taskResultSummaryRepository,
								TaskResultFileRepository taskResultFileRepository,
								TaskDomainService taskDomainService,
								TaskStatusDomainService taskStatusDomainService,
								TaskValidationDomainService taskValidationDomainService,
								TaskValidationManager taskValidationManager,
								TaskFileStorageService taskFileStorageService,
								TaskParamSchemaValidator taskParamSchemaValidator,
								TaskAssembler taskAssembler,
								TaskNoGenerator taskNoGenerator,
								SchedulerClient schedulerClient,
								SolverClient solverClient,
								TaskStoragePathSupport taskStoragePathSupport) {
		this.taskRepository = taskRepository;
		this.taskFileRepository = taskFileRepository;
		this.taskStatusHistoryRepository = taskStatusHistoryRepository;
		this.taskLogRepository = taskLogRepository;
		this.taskResultSummaryRepository = taskResultSummaryRepository;
		this.taskResultFileRepository = taskResultFileRepository;
		this.taskDomainService = taskDomainService;
		this.taskStatusDomainService = taskStatusDomainService;
		this.taskValidationDomainService = taskValidationDomainService;
		this.taskValidationManager = taskValidationManager;
		this.taskFileStorageService = taskFileStorageService;
		this.taskParamSchemaValidator = taskParamSchemaValidator;
		this.taskAssembler = taskAssembler;
		this.taskNoGenerator = taskNoGenerator;
		this.schedulerClient = schedulerClient;
		this.solverClient = solverClient;
		this.taskStoragePathSupport = taskStoragePathSupport;
	}

	public TaskCreateResponse createTask(CreateTaskRequest request, Long userId) {
		validateTaskDefinition(request.getSolverId(), request.getProfileId(), request.getTaskType());
		validateTaskParams(request.getProfileId(), request.getParams(), null, TaskStatusEnum.CREATED.name());
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
			validateTaskParams(task.getProfileId(), request.getParams(), task.getId(), task.getStatus());
			paramsChanged = !Objects.equals(parseParamsJson(task.getParamsJson()), request.getParams());
			task.setParamsJson(JsonUtil.toJson(request.getParams()));
		}
		if (paramsChanged && TaskStatusEnum.VALIDATED.name().equals(task.getStatus())) {
			taskStatusDomainService.transfer(task, TaskStatusEnum.CREATED.name(), "task parameters updated, re-validation required", OperatorTypeEnum.USER.name(), userId);
		}
		taskRepository.update(task);
		return taskAssembler.toUpdateResponse(task);
	}

	@Transactional
	public TaskFileUploadResponse uploadTaskFile(Long taskId, MultipartFile file, String fileKey, String fileRole, Long userId) {
		Task task = loadAndCheckOwner(taskId, userId);
		taskValidationDomainService.checkTaskEditable(task);
		UploadConstraint constraint = resolveUploadConstraint(task);
		validateArchiveUpload(task, file, constraint);
		TaskFile existingArchive = findExistingArchive(task, constraint);
		TaskFile taskFile = taskFileStorageService.saveInputFile(taskId, file, constraint.fileKey(), constraint.fileRole());
		taskFile.setArchiveFlag(1);
		try {
			taskFileRepository.save(taskFile);
			finishArchiveReplacement(task, existingArchive, taskFile, userId);
		} catch (RuntimeException ex) {
			rollbackUploadedFileOnFailure(existingArchive, taskFile);
			throw ex;
		}
		TaskFileUploadResponse response = toTaskFileUploadResponse(taskFile);
		response.setStatus(task.getStatus());
		return response;
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
		validateTaskDefinition(task.getSolverId(), task.getProfileId(), task.getTaskType());
		taskStatusDomainService.transfer(task, TaskStatusEnum.QUEUED.name(), "task submitted", OperatorTypeEnum.USER.name(), userId);
		taskRepository.update(task);
		schedulerClient.notifyTaskSubmitted(taskId);
		return buildTaskSubmitResponse(task);
	}

	public TaskActionResponse cancelTask(Long taskId, Long userId, String reason) {
		Task task = loadAndCheckOwner(taskId, userId);
		if (TaskStatusEnum.CANCELED.name().equals(task.getStatus())) {
			return buildTaskActionResponse(task);
		}
		if (!taskDomainService.canCancel(task)) {
			throw new BizException(ErrorCodeConstants.TASK_CANCEL_NOT_ALLOWED, "task cannot be canceled in current status");
		}
		taskStatusDomainService.transfer(task, TaskStatusEnum.CANCELED.name(), reason, OperatorTypeEnum.USER.name(), userId);
		taskRepository.update(task);
		return buildTaskActionResponse(task);
	}

	public TaskPriorityUpdateResponse adjustPriority(Long taskId, Integer priority, Long adminUserId) {
		Task task = loadTask(taskId);
		if (!List.of(TaskStatusEnum.CREATED.name(), TaskStatusEnum.VALIDATED.name(), TaskStatusEnum.QUEUED.name())
				.contains(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_PRIORITY_UPDATE_NOT_ALLOWED,
					"priority can only be adjusted in CREATED, VALIDATED or QUEUED status");
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
		return taskAssembler.toPriorityUpdateResponse(task);
	}

	public TaskActionResponse retryTask(Long taskId, String reason, Long adminUserId) {
		Task task = loadTask(taskId);
		if (!TaskStatusEnum.FAILED.name().equals(task.getStatus()) && !TaskStatusEnum.TIMEOUT.name().equals(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_RETRY_NOT_ALLOWED, "only failed or timeout tasks can be retried");
		}

		resetTaskExecutionArtifacts(task.getId());
		taskValidationManager.rebuildValidatedWorkspace(task.getId());
		task = loadTask(taskId);
		String effectiveReason = reason == null || reason.isBlank() ? "admin retried task" : reason;
		taskStatusDomainService.transfer(task, TaskStatusEnum.QUEUED.name(), effectiveReason, OperatorTypeEnum.ADMIN.name(), adminUserId);
		taskRepository.update(task);
		schedulerClient.notifyTaskSubmitted(task.getId());

		return buildTaskActionResponse(task);
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
	public TaskStatusAckDTO reportStatus(Long taskId, StatusReportRequest request) {
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (request == null || request.getNodeId() == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "status report request is required");
		}
		if (task.getNodeId() == null) {
			throw new BizException(ErrorCodeConstants.TASK_NOT_BOUND_TO_NODE, "task is not bound to node");
		}
		if (!task.getNodeId().equals(request.getNodeId())) {
			throw new BizException(ErrorCodeConstants.REPORTED_NODE_MISMATCH, "reported node does not match task bound node");
		}
		String targetStatus = pickStatus(request);
		String reason = pickReason(request);
		if (!TaskStatusEnum.RUNNING.name().equalsIgnoreCase(targetStatus)) {
			throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "status-report only supports RUNNING");
		}
		if (isIdempotentRunningReport(task, request, targetStatus)) {
			releaseReservation(task.getNodeId(), task.getId());
			return buildTaskStatusAck(task);
		}
		if (request.getFromStatus() != null && !request.getFromStatus().isBlank() && !request.getFromStatus().equalsIgnoreCase(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_STATUS_MISMATCH, "task status mismatch");
		}
		if (!TaskStatusEnum.DISPATCHED.name().equals(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status for running report: " + task.getStatus());
		}
		taskStatusDomainService.transfer(task, TaskStatusEnum.RUNNING.name(), reason, OperatorTypeEnum.NODE.name(), null);
		taskRepository.update(task);
		releaseReservation(task.getNodeId(), task.getId());
		return buildTaskStatusAck(task);
	}

	private String pickStatus(StatusReportRequest request) {
		if (request == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "status report request is required");
		}
		if (request.getToStatus() != null && !request.getToStatus().isBlank()) {
			return request.getToStatus();
		}
		throw new BizException(ErrorCodeConstants.BAD_REQUEST, "status is required");
	}

	private String pickReason(StatusReportRequest request) {
		if (request == null) {
			return null;
		}
		return request.getChangeReason();
	}

	private boolean isIdempotentRunningReport(Task task, StatusReportRequest request, String targetStatus) {
		if (task == null || request == null || request.getNodeId() == null) {
			return false;
		}
		if (!TaskStatusEnum.RUNNING.name().equalsIgnoreCase(targetStatus)) {
			return false;
		}
		if (!TaskStatusEnum.RUNNING.name().equalsIgnoreCase(task.getStatus())) {
			return false;
		}
		String fromStatus = request.getFromStatus();
		if (fromStatus == null || fromStatus.isBlank()) {
			return true;
		}
		return TaskStatusEnum.DISPATCHED.name().equalsIgnoreCase(fromStatus)
				|| TaskStatusEnum.RUNNING.name().equalsIgnoreCase(fromStatus);
	}

	private TaskFileUploadResponse toTaskFileUploadResponse(TaskFile file) {
		TaskFileUploadResponse response = new TaskFileUploadResponse();
		response.setFileId(file.getId());
		response.setOriginName(file.getOriginName());
		response.setStoragePath(taskStoragePathSupport.toDisplayTaskPath(file.getStoragePath()));
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
		boolean suffixAllowed = constraint.allowSuffix().stream()
				.map(this::normalizeSuffix)
				.anyMatch(v -> v.equals(suffix));
		if (!suffixAllowed) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "only zip archive is supported");
		}
		long maxBytes = (long) constraint.maxSizeMb() * 1024 * 1024;
		if (file.getSize() > maxBytes) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "archive size exceeds limit");
		}
	}

	private TaskFile findExistingArchive(Task task, UploadConstraint constraint) {
		return taskFileRepository
				.findByTaskIdAndFileRoleAndFileKey(task.getId(), constraint.fileRole(), constraint.fileKey())
				.orElse(null);
	}

	private void finishArchiveReplacement(Task task, TaskFile existingArchive, TaskFile uploadedArchive, Long userId) {
		if (uploadedArchive == null) {
			return;
		}
		if (TaskStatusEnum.VALIDATED.name().equals(task.getStatus())) {
			taskStatusDomainService.transfer(task, TaskStatusEnum.CREATED.name(), "input archive replaced, re-validation required", OperatorTypeEnum.USER.name(), userId);
			taskRepository.update(task);
		}
		deleteOtherTaskFiles(task.getId(), uploadedArchive.getId());
		taskFileStorageService.deleteTaskPreparedArtifacts(task.getId());
		if (existingArchive != null && !sameStoragePath(existingArchive.getStoragePath(), uploadedArchive.getStoragePath())) {
			deleteStoredFileQuietly(existingArchive.getStoragePath());
		}
	}

	private void rollbackUploadedFileOnFailure(TaskFile existingArchive, TaskFile uploadedArchive) {
		if (uploadedArchive == null) {
			return;
		}
		if (existingArchive != null && sameStoragePath(existingArchive.getStoragePath(), uploadedArchive.getStoragePath())) {
			return;
		}
		deleteStoredFileQuietly(uploadedArchive.getStoragePath());
	}

	private void deleteStoredFileQuietly(String storagePath) {
		if (storagePath == null || storagePath.isBlank()) {
			return;
		}
		try {
			taskFileStorageService.deleteFile(storagePath);
		} catch (RuntimeException ex) {
			log.warn("failed to delete uploaded file after persistence error, path={}", storagePath, ex);
		}
	}

	private void deleteOtherTaskFiles(Long taskId, Long keepId) {
		for (TaskFile taskFile : taskFileRepository.listByTaskId(taskId)) {
			if (keepId != null && keepId.equals(taskFile.getId())) {
				continue;
			}
			taskFileRepository.deleteById(taskFile.getId());
		}
	}

	private boolean sameStoragePath(String left, String right) {
		if (left == null || right == null) {
			return false;
		}
		return left.equals(right);
	}

	private void discardTaskInternal(Task task, String reason) {
		deleteOtherTaskFiles(task.getId(), null);
		taskFileStorageService.deleteTaskArtifacts(task.getId());
		task.setDeletedFlag(1);
		taskRepository.update(task);
	}

	private void resetTaskExecutionArtifacts(Long taskId) {
		taskLogRepository.deleteByTaskId(taskId);
		taskResultFileRepository.deleteByTaskId(taskId);
		taskResultSummaryRepository.deleteByTaskId(taskId);
		taskFileStorageService.deleteTaskRuntimeArtifacts(taskId);
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

	private String normalizeSuffix(String suffix) {
		if (suffix == null) {
			return "";
		}
		String normalized = suffix.trim().toLowerCase(Locale.ROOT);
		while (normalized.startsWith(".")) {
			normalized = normalized.substring(1);
		}
		return normalized;
	}

	private TaskStatusAckDTO buildTaskStatusAck(Task task) {
		TaskStatusAckDTO response = new TaskStatusAckDTO();
		response.setTaskId(task == null ? null : task.getId());
		response.setStatus(task == null ? null : task.getStatus());
		return response;
	}

	private TaskSubmitResponse buildTaskSubmitResponse(Task task) {
		TaskSubmitResponse response = new TaskSubmitResponse();
		response.setTaskId(task == null ? null : task.getId());
		response.setStatus(task == null ? null : task.getStatus());
		response.setSubmitTime(task == null ? null : task.getSubmitTime());
		return response;
	}

	private TaskActionResponse buildTaskActionResponse(Task task) {
		TaskActionResponse response = new TaskActionResponse();
		response.setTaskId(task == null ? null : task.getId());
		response.setStatus(task == null ? null : task.getStatus());
		return response;
	}

	private void validateTaskParams(Long profileId, Map<String, Object> params, Long taskId, String status) {
		String schemaText = solverClient.getProfileParamsSchema(profileId);
		List<com.example.cae.task.interfaces.response.TaskValidateResponse.ValidationIssue> issues =
				taskParamSchemaValidator.validatePartial(schemaText, params);
		if (issues.isEmpty()) {
			return;
		}
		com.example.cae.task.interfaces.response.TaskValidateResponse response = new com.example.cae.task.interfaces.response.TaskValidateResponse();
		response.setTaskId(taskId);
		response.setValid(Boolean.FALSE);
		response.setStatus(status);
		response.setIssues(issues);
		throw new BizException(ErrorCodeConstants.TASK_VALIDATION_FAILED, "task parameter validation failed", response);
	}

	private void validateTaskDefinition(Long solverId, Long profileId, String taskType) {
		SolverClient.SolverMeta solverMeta = solverClient.getSolverMeta(solverId);
		if (solverMeta == null) {
			throw new BizException(ErrorCodeConstants.SOLVER_NOT_FOUND, "solver not found");
		}
		if (!Integer.valueOf(1).equals(solverMeta.getEnabled())) {
			throw new BizException(ErrorCodeConstants.SOLVER_DISABLED, "solver disabled");
		}
		SolverClient.ProfileMeta profileMeta = solverClient.getProfileMeta(profileId);
		if (profileMeta == null) {
			throw new BizException(ErrorCodeConstants.PROFILE_NOT_FOUND, "profile not found");
		}
		if (!Integer.valueOf(1).equals(profileMeta.getEnabled())) {
			throw new BizException(ErrorCodeConstants.PROFILE_DISABLED, "profile disabled");
		}
		if (!solverId.equals(profileMeta.getSolverId())) {
			throw new BizException(ErrorCodeConstants.TASK_PROFILE_MISMATCH, "solver and profile do not match");
		}
		if (profileMeta.getTaskType() != null && !profileMeta.getTaskType().isBlank()
				&& !profileMeta.getTaskType().equals(taskType)) {
			throw new BizException(ErrorCodeConstants.TASK_TYPE_MISMATCH, "task type and profile do not match");
		}
	}

	private void releaseReservation(Long nodeId, Long taskId) {
		if (nodeId == null || taskId == null) {
			return;
		}
		schedulerClient.releaseNodeReservation(nodeId, taskId);
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

