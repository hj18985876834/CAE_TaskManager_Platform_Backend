package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskStatusAckDTO;
import com.example.cae.common.enums.OperatorTypeEnum;
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
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.request.ResultFileReportRequest;
import com.example.cae.task.interfaces.request.ResultSummaryReportRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class TaskResultManager {
	private final TaskRepository taskRepository;
	private final TaskLogRepository taskLogRepository;
	private final TaskResultSummaryRepository taskResultSummaryRepository;
	private final TaskResultFileRepository taskResultFileRepository;
	private final TaskStatusDomainService taskStatusDomainService;
	private final TaskStoragePathSupport taskStoragePathSupport;

	public TaskResultManager(TaskRepository taskRepository,
							 TaskLogRepository taskLogRepository,
							 TaskResultSummaryRepository taskResultSummaryRepository,
							 TaskResultFileRepository taskResultFileRepository,
							 TaskStatusDomainService taskStatusDomainService,
							 TaskStoragePathSupport taskStoragePathSupport) {
		this.taskRepository = taskRepository;
		this.taskLogRepository = taskLogRepository;
		this.taskResultSummaryRepository = taskResultSummaryRepository;
		this.taskResultFileRepository = taskResultFileRepository;
		this.taskStatusDomainService = taskStatusDomainService;
		this.taskStoragePathSupport = taskStoragePathSupport;
	}

	public void appendLog(Long taskId, Integer seqNo, String content) {
		ensureLogReportAllowed(taskId);
		TaskLogChunk chunk = new TaskLogChunk();
		chunk.setTaskId(taskId);
		chunk.setSeqNo(seqNo);
		chunk.setLogContent(content);
		taskLogRepository.save(chunk);
	}

	public void saveResultSummary(Long taskId, ResultSummaryReportRequest request) {
		ensureResultSummaryAllowed(taskId);
		TaskResultSummary summary = new TaskResultSummary();
		summary.setTaskId(taskId);
		Integer successFlag = request.getSuccessFlag();
		if (successFlag == null) {
			successFlag = Boolean.TRUE.equals(request.getSuccess()) ? 1 : 0;
		}
		summary.setSuccessFlag(successFlag);
		summary.setDurationSeconds(request.getDurationSeconds());
		summary.setSummaryText(request.getSummaryText());
		summary.setMetricsJson(request.getMetrics() == null ? null : JsonUtil.toJson(request.getMetrics()));
		taskResultSummaryRepository.saveOrUpdate(summary);
	}

	public void saveResultFile(Long taskId, ResultFileReportRequest request) {
		ensureResultFileAllowed(taskId);
		TaskResultFile file = new TaskResultFile();
		file.setTaskId(taskId);
		file.setFileType(request.getFileType());
		file.setFileName(request.getFileName());
		file.setStoragePath(taskStoragePathSupport.toStoredResultPath(request.getStoragePath()));
		file.setFileSize(request.getFileSize());
		taskResultFileRepository.save(file);
	}

	@Transactional
	public TaskStatusAckDTO finishTask(Long taskId, String finalStatus) {
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		String target = normalizeAllowedFinalStatus(finalStatus);
		if (shouldIgnoreTerminalReport(task, target)) {
			return buildTaskStatusAck(task);
		}
		if (!TaskStatusEnum.RUNNING.name().equals(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status for terminal report: " + task.getStatus());
		}
		taskStatusDomainService.transfer(task, target, "task finished", OperatorTypeEnum.NODE.name(), null);
		taskRepository.update(task);
		return buildTaskStatusAck(task);
	}

	@Transactional
	public TaskStatusAckDTO failTask(Long taskId, String failType, String failMessage) {
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		String normalizedFailType = normalizeAllowedFailType(failType);
		String targetStatus = TaskStatusEnum.FAILED.name();
		if (shouldIgnoreTerminalReport(task, targetStatus)) {
			return buildTaskStatusAck(task);
		}
		if (!TaskStatusEnum.RUNNING.name().equals(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status for fail report: " + task.getStatus());
		}
		task.setFailType(normalizedFailType);
		task.setFailMessage(failMessage);
		taskStatusDomainService.transfer(task, targetStatus, failMessage, OperatorTypeEnum.NODE.name(), null);
		taskRepository.update(task);
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
		if (TaskStatusEnum.TIMEOUT.name().equals(normalized)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST,
					"failType TIMEOUT is not supported by mark-failed, use mark-finished with finalStatus=TIMEOUT");
		}
		return normalized;
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

	private void ensureResultSummaryAllowed(Long taskId) {
		Task task = loadTask(taskId);
		if (task.isFinished() || TaskStatusEnum.RUNNING.name().equals(task.getStatus())) {
			return;
		}
		throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL,
				"illegal status for result summary report: " + task.getStatus());
	}

	private void ensureResultFileAllowed(Long taskId) {
		Task task = loadTask(taskId);
		if (task.isFinished() || TaskStatusEnum.RUNNING.name().equals(task.getStatus())) {
			return;
		}
		throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL,
				"illegal status for result file report: " + task.getStatus());
	}

	private Task loadTask(Long taskId) {
		return taskRepository.findById(taskId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
	}

	private TaskStatusAckDTO buildTaskStatusAck(Task task) {
		TaskStatusAckDTO response = new TaskStatusAckDTO();
		response.setTaskId(task == null ? null : task.getId());
		response.setStatus(task == null ? null : task.getStatus());
		return response;
	}
}
