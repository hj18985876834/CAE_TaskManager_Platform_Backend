package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskStatusAckDTO;
import com.example.cae.common.enums.FailTypeEnum;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.ResultFileTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.utils.JsonUtil;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskLogChunk;
import com.example.cae.task.domain.model.TaskResultFile;
import com.example.cae.task.domain.model.TaskResultSummary;
import com.example.cae.task.domain.repository.TaskLogRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskResultFileRepository;
import com.example.cae.task.domain.repository.TaskResultSummaryRepository;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.support.TaskPathResolver;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.request.ResultFileReportRequest;
import com.example.cae.task.interfaces.request.ResultSummaryReportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Set;

@Service
public class TaskResultManager {
	private static final Logger log = LoggerFactory.getLogger(TaskResultManager.class);
	private static final int MAX_HISTORY_REASON_LENGTH = 255;
	private static final Set<String> RESULT_REPORT_ALLOWED_STATUSES = Set.of(
			TaskStatusEnum.RUNNING.name(),
			TaskStatusEnum.SUCCESS.name(),
			TaskStatusEnum.TIMEOUT.name()
	);
	private final TaskRepository taskRepository;
	private final TaskLogRepository taskLogRepository;
	private final TaskResultSummaryRepository taskResultSummaryRepository;
	private final TaskResultFileRepository taskResultFileRepository;
	private final TaskStatusHistoryRepository taskStatusHistoryRepository;
	private final TaskStatusDomainService taskStatusDomainService;
	private final SchedulerClient schedulerClient;
	private final TaskStoragePathSupport taskStoragePathSupport;
	private final TaskPathResolver taskPathResolver;

	public TaskResultManager(TaskRepository taskRepository,
							 TaskLogRepository taskLogRepository,
							 TaskResultSummaryRepository taskResultSummaryRepository,
							 TaskResultFileRepository taskResultFileRepository,
							 TaskStatusHistoryRepository taskStatusHistoryRepository,
							 TaskStatusDomainService taskStatusDomainService,
							 SchedulerClient schedulerClient,
							 TaskStoragePathSupport taskStoragePathSupport,
							 TaskPathResolver taskPathResolver) {
		this.taskRepository = taskRepository;
		this.taskLogRepository = taskLogRepository;
		this.taskResultSummaryRepository = taskResultSummaryRepository;
		this.taskResultFileRepository = taskResultFileRepository;
		this.taskStatusHistoryRepository = taskStatusHistoryRepository;
		this.taskStatusDomainService = taskStatusDomainService;
		this.schedulerClient = schedulerClient;
		this.taskStoragePathSupport = taskStoragePathSupport;
		this.taskPathResolver = taskPathResolver;
	}

	public void appendLog(Long taskId, Integer seqNo, String content) {
		ensureLogReportAllowed(taskId);
		TaskLogChunk chunk = new TaskLogChunk();
		chunk.setTaskId(taskId);
		chunk.setSeqNo(seqNo);
		chunk.setLogContent(content);
		taskLogRepository.save(chunk);
	}

	@Transactional(noRollbackFor = BizException.class)
	public void saveResultSummary(Long taskId, ResultSummaryReportRequest request) {
		ensureResultSummaryAllowed(loadTaskForResultWrite(taskId));
		TaskResultSummary summary = new TaskResultSummary();
		summary.setTaskId(taskId);
		summary.setSuccessFlag(request.getSuccessFlag());
		summary.setDurationSeconds(request.getDurationSeconds());
		summary.setSummaryText(request.getSummaryText());
		summary.setMetricsJson(request.getMetrics() == null ? null : JsonUtil.toJson(request.getMetrics()));
		taskResultSummaryRepository.saveOrUpdate(summary);
	}

	@Transactional(noRollbackFor = BizException.class)
	public void saveResultFile(Long taskId, ResultFileReportRequest request) {
		ensureResultFileAllowed(loadTaskForResultWrite(taskId));
		Path resultPath = validateResultFilePath(taskId, request);
		TaskResultFile file = new TaskResultFile();
		file.setTaskId(taskId);
		file.setFileType(normalizeResultFileType(request.getFileType()));
		file.setFileName(request.getFileName().trim());
		file.setStoragePath(taskStoragePathSupport.toStoredResultPath(resultPath.toString()));
		file.setFileSize(readActualFileSize(resultPath));
		taskResultFileRepository.save(file);
	}

	@Transactional
	public TaskStatusAckDTO finishTask(Long taskId, String finalStatus) {
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		String target = normalizeAllowedFinalStatus(finalStatus);
		if (shouldIgnoreTerminalReport(task, target)) {
			recordIgnoredTerminalCallback(task, "mark-finished", target, null);
			releaseReservationQuietly(task);
			return buildTaskStatusAck(task);
		}
		if (!TaskStatusEnum.RUNNING.name().equals(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status for terminal report: " + task.getStatus());
		}
		taskStatusDomainService.transfer(task, target, "task finished", OperatorTypeEnum.NODE.name(), null);
		taskRepository.update(task);
		releaseReservationQuietly(task);
		return buildTaskStatusAck(task);
	}

	@Transactional
	public TaskStatusAckDTO failTask(Long taskId, String failType, String failMessage) {
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		String normalizedFailType = normalizeAllowedFailType(failType);
		String targetStatus = TaskStatusEnum.FAILED.name();
		if (shouldIgnoreTerminalReport(task, targetStatus)) {
			recordIgnoredTerminalCallback(task, "mark-failed", targetStatus, normalizedFailType);
			releaseReservationQuietly(task);
			return buildTaskStatusAck(task);
		}
		if (!TaskStatusEnum.RUNNING.name().equals(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status for fail report: " + task.getStatus());
		}
		task.setFailType(normalizedFailType);
		task.setFailMessage(failMessage);
		taskStatusDomainService.transfer(task, targetStatus, failMessage, OperatorTypeEnum.NODE.name(), null);
		taskRepository.update(task);
		cleanupResultArtifactsIfNeeded(taskId, normalizedFailType);
		releaseReservationQuietly(task);
		return buildTaskStatusAck(task);
	}

	private boolean shouldIgnoreTerminalReport(Task task, String targetStatus) {
		if (task == null || targetStatus == null) {
			return false;
		}
		if (targetStatus.equalsIgnoreCase(task.getStatus())) {
			return true;
		}
		return task.isFinished();
	}


	private String normalizeResultFileType(String fileType) {
		String normalized = fileType == null || fileType.isBlank()
				? null
				: fileType.trim().toUpperCase();
		if (normalized == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "result fileType is required");
		}
		try {
			return ResultFileTypeEnum.valueOf(normalized).name();
		} catch (IllegalArgumentException ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "unsupported result fileType: " + fileType);
		}
	}
	private String normalizeAllowedFinalStatus(String finalStatus) {

		String target = finalStatus == null || finalStatus.isBlank()
				? TaskStatusEnum.SUCCESS.name()
				: finalStatus.trim().toUpperCase();
		if (!Set.of(TaskStatusEnum.SUCCESS.name(), TaskStatusEnum.TIMEOUT.name()).contains(target)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "unsupported finalStatus: " + target);
		}
		return target;
	}

	private String normalizeAllowedFailType(String failType) {
		String normalized = failType == null || failType.isBlank()
				? null
				: failType.trim().toUpperCase();
		if (normalized == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "failType is required");
		}
		try {
			FailTypeEnum failTypeEnum = FailTypeEnum.valueOf(normalized);
			if (FailTypeEnum.TIMEOUT == failTypeEnum) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST,
						"failType TIMEOUT is not supported by mark-failed, use mark-finished with finalStatus=TIMEOUT");
			}
			return failTypeEnum.name();
		} catch (IllegalArgumentException ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "unsupported failType: " + failType);
		}
	}

	private void cleanupResultArtifactsIfNeeded(Long taskId, String failType) {
		if (taskId == null || failType == null) {
			return;
		}
		if (!FailTypeEnum.CALLBACK_ERROR.name().equalsIgnoreCase(failType)) {
			return;
		}
		try {
			taskResultSummaryRepository.deleteByTaskId(taskId);
			taskResultFileRepository.deleteByTaskId(taskId);
		} catch (Exception ex) {
			log.warn("callback error terminal state committed but result metadata cleanup failed, taskId={}", taskId, ex);
		}
	}

	private void recordIgnoredTerminalCallback(Task task, String action, String requestedStatus, String requestedFailType) {
		if (task == null || task.getId() == null || task.getStatus() == null || !task.isFinished()) {
			return;
		}
		String requested = requestedStatus == null || requestedStatus.isBlank() ? "UNKNOWN" : requestedStatus.trim().toUpperCase();
		String effectiveAction = action == null || action.isBlank() ? "terminal-callback" : action.trim();
		String reason = buildIgnoredTerminalCallbackReason(effectiveAction, requested, requestedFailType, task.getStatus());
		com.example.cae.task.domain.model.TaskStatusHistory history = new com.example.cae.task.domain.model.TaskStatusHistory();
		history.setTaskId(task.getId());
		history.setFromStatus(task.getStatus());
		history.setToStatus(task.getStatus());
		history.setChangeReason(reason);
		history.setOperatorType(OperatorTypeEnum.NODE.name());
		history.setOperatorId(null);
		taskStatusHistoryRepository.save(history);
	}

	private String buildIgnoredTerminalCallbackReason(String action, String requestedStatus, String requestedFailType, String currentStatus) {
		StringBuilder builder = new StringBuilder("ignored late ");
		builder.append(action);
		builder.append(", requested=");
		builder.append(requestedStatus);
		if (requestedFailType != null && !requestedFailType.isBlank()) {
			builder.append('(').append(requestedFailType.trim().toUpperCase()).append(')');
		}
		builder.append(", current=");
		builder.append(currentStatus == null ? "UNKNOWN" : currentStatus);
		String reason = builder.toString();
		return reason.length() <= MAX_HISTORY_REASON_LENGTH
				? reason
				: reason.substring(0, MAX_HISTORY_REASON_LENGTH);
	}

	private void ensureLogReportAllowed(Long taskId) {
		Task task = loadTask(taskId);
		if (task.isFinished()) {
			return;
		}
		if (TaskStatusEnum.DISPATCHED.name().equals(task.getStatus())
				|| TaskStatusEnum.RUNNING.name().equals(task.getStatus())) {
			return;
		}
		throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL,
				"illegal status for log report: " + task.getStatus());
	}

	private void ensureResultSummaryAllowed(Task task) {
		if (RESULT_REPORT_ALLOWED_STATUSES.contains(task.getStatus())) {
			return;
		}
		recordRejectedResultCallbackIfNeeded(task, "result-summary-report");
		throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL,
				"illegal status for result summary report: " + task.getStatus());
	}

	private void ensureResultFileAllowed(Task task) {
		if (RESULT_REPORT_ALLOWED_STATUSES.contains(task.getStatus())) {
			return;
		}
		recordRejectedResultCallbackIfNeeded(task, "result-file-report");
		throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL,
				"illegal status for result file report: " + task.getStatus());
	}

	private void recordRejectedResultCallbackIfNeeded(Task task, String action) {
		if (task == null || task.getId() == null || task.getStatus() == null || !task.isFinished()) {
			return;
		}
		com.example.cae.task.domain.model.TaskStatusHistory history = new com.example.cae.task.domain.model.TaskStatusHistory();
		history.setTaskId(task.getId());
		history.setFromStatus(task.getStatus());
		history.setToStatus(task.getStatus());
		history.setChangeReason(buildRejectedResultCallbackReason(action, task.getStatus()));
		history.setOperatorType(OperatorTypeEnum.NODE.name());
		history.setOperatorId(null);
		taskStatusHistoryRepository.save(history);
	}

	private String buildRejectedResultCallbackReason(String action, String currentStatus) {
		String effectiveAction = action == null || action.isBlank() ? "result-report" : action.trim();
		String reason = "ignored late " + effectiveAction + ", current="
				+ (currentStatus == null ? "UNKNOWN" : currentStatus);
		return reason.length() <= MAX_HISTORY_REASON_LENGTH
				? reason
				: reason.substring(0, MAX_HISTORY_REASON_LENGTH);
	}

	private Task loadTaskForResultWrite(Long taskId) {
		return taskRepository.findByIdForUpdate(taskId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
	}

	private Task loadTask(Long taskId) {
		return taskRepository.findById(taskId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
	}

	private void releaseReservationQuietly(Task task) {
		if (task == null || task.getNodeId() == null || task.getId() == null) {
			return;
		}
		try {
			schedulerClient.releaseNodeReservation(task.getNodeId(), task.getId());
		} catch (Exception ex) {
			log.warn("terminal state updated but residual reservation release failed, nodeId={}, taskId={}",
					task.getNodeId(), task.getId(), ex);
			recordReservationReleaseFailure(task, ex);
		}
	}

	private void recordReservationReleaseFailure(Task task, Exception ex) {
		try {
			schedulerClient.recordScheduleFailure(
					task.getId(),
					task.getNodeId(),
					buildReservationReleaseFailureMessage(ex)
			);
		} catch (Exception recordEx) {
			log.warn("failed to record terminal reservation cleanup failure, nodeId={}, taskId={}",
					task == null ? null : task.getNodeId(),
					task == null ? null : task.getId(),
					recordEx);
		}
	}

	private String buildReservationReleaseFailureMessage(Exception ex) {
		String message = ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
				? "reservation release failed after terminal state transition"
				: ex.getMessage();
		return "reservation release failed after terminal state transition: " + message;
	}

	private Path validateResultFilePath(Long taskId, ResultFileReportRequest request) {
		String fileName = request.getFileName() == null ? "" : request.getFileName().trim();
		if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\") || containsControlCharacter(fileName)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "result fileName must be a plain file name");
		}
		Path resultDir = Path.of(taskPathResolver.resolveResultDir(taskId)).toAbsolutePath().normalize();
		Path resultPath;
		try {
			resultPath = Path.of(taskStoragePathSupport.toAbsoluteResultPath(request.getStoragePath()))
					.toAbsolutePath()
					.normalize();
		} catch (InvalidPathException ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "result storagePath is invalid");
		}
		if (!resultPath.startsWith(resultDir)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "result storagePath must be under task output directory");
		}
		if (!Files.exists(resultPath) || !Files.isRegularFile(resultPath)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "result file does not exist");
		}
		return resultPath;
	}

	private long readActualFileSize(Path resultPath) {
		try {
			return Files.size(resultPath);
		} catch (Exception ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "result file cannot be accessed");
		}
	}

	private boolean containsControlCharacter(String value) {
		for (int i = 0; i < value.length(); i++) {
			if (Character.isISOControl(value.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	private TaskStatusAckDTO buildTaskStatusAck(Task task) {
		TaskStatusAckDTO response = new TaskStatusAckDTO();
		response.setTaskId(task == null ? null : task.getId());
		response.setStatus(task == null ? null : task.getStatus());
		return response;
	}
}
