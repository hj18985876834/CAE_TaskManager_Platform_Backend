package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
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

import java.util.Locale;
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
		TaskLogChunk chunk = new TaskLogChunk();
		chunk.setTaskId(taskId);
		chunk.setSeqNo(seqNo);
		chunk.setLogContent(content);
		taskLogRepository.save(chunk);
	}

	public void saveResultSummary(Long taskId, ResultSummaryReportRequest request) {
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
		TaskResultFile file = new TaskResultFile();
		file.setTaskId(taskId);
		file.setFileType(request.getFileType());
		file.setFileName(request.getFileName());
		file.setStoragePath(taskStoragePathSupport.toStoredResultPath(request.getStoragePath()));
		file.setFileSize(request.getFileSize());
		taskResultFileRepository.save(file);
	}

	@Transactional
	public void finishTask(Long taskId, String finalStatus) {
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		String target = finalStatus == null || finalStatus.isBlank() ? TaskStatusEnum.SUCCESS.name() : finalStatus;
		if (shouldIgnoreTerminalReport(task, target)) {
			return;
		}
		promoteScheduledTaskForNodeTerminal(task, target);
		taskStatusDomainService.transfer(task, target, "task finished", OperatorTypeEnum.NODE.name(), null);
		taskRepository.update(task);
	}

	@Transactional
	public void failTask(Long taskId, String failType, String failMessage) {
		Task task = taskRepository.findByIdForUpdate(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (shouldIgnoreTerminalReport(task, TaskStatusEnum.FAILED.name())) {
			return;
		}
		promoteScheduledTaskForNodeTerminal(task, TaskStatusEnum.FAILED.name());
		task.setFailType(failType);
		task.setFailMessage(failMessage);
		taskStatusDomainService.transfer(task, TaskStatusEnum.FAILED.name(), failMessage, OperatorTypeEnum.NODE.name(), null);
		taskRepository.update(task);
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

	private void promoteScheduledTaskForNodeTerminal(Task task, String targetStatus) {
		if (task == null || targetStatus == null) {
			return;
		}
		if (!TaskStatusEnum.SCHEDULED.name().equals(task.getStatus())) {
			return;
		}
		if (!Set.of(TaskStatusEnum.SUCCESS.name(), TaskStatusEnum.FAILED.name(),
				TaskStatusEnum.TIMEOUT.name(), TaskStatusEnum.CANCELED.name()).contains(targetStatus.toUpperCase(Locale.ROOT))) {
			return;
		}
		taskStatusDomainService.transfer(task, TaskStatusEnum.DISPATCHED.name(), "dispatch acknowledged by node result report", OperatorTypeEnum.SYSTEM.name(), null);
	}
}
