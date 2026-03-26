package com.example.cae.task.application.manager;

import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskLogChunk;
import com.example.cae.task.domain.model.TaskResultFile;
import com.example.cae.task.domain.model.TaskResultSummary;
import com.example.cae.task.domain.repository.TaskLogRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskResultFileRepository;
import com.example.cae.task.domain.repository.TaskResultSummaryRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.interfaces.request.ResultFileReportRequest;
import com.example.cae.task.interfaces.request.ResultSummaryReportRequest;
import org.springframework.stereotype.Service;

@Service
public class TaskResultManager {
	private final TaskRepository taskRepository;
	private final TaskLogRepository taskLogRepository;
	private final TaskResultSummaryRepository taskResultSummaryRepository;
	private final TaskResultFileRepository taskResultFileRepository;
	private final TaskStatusDomainService taskStatusDomainService;

	public TaskResultManager(TaskRepository taskRepository, TaskLogRepository taskLogRepository, TaskResultSummaryRepository taskResultSummaryRepository, TaskResultFileRepository taskResultFileRepository, TaskStatusDomainService taskStatusDomainService) {
		this.taskRepository = taskRepository;
		this.taskLogRepository = taskLogRepository;
		this.taskResultSummaryRepository = taskResultSummaryRepository;
		this.taskResultFileRepository = taskResultFileRepository;
		this.taskStatusDomainService = taskStatusDomainService;
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
		summary.setSuccessFlag(Boolean.TRUE.equals(request.getSuccess()) ? 1 : 0);
		summary.setDurationSeconds(request.getDurationSeconds());
		summary.setSummaryText(request.getSummaryText());
		summary.setMetricsJson(String.valueOf(request.getMetrics()));
		taskResultSummaryRepository.saveOrUpdate(summary);
	}

	public void saveResultFile(Long taskId, ResultFileReportRequest request) {
		TaskResultFile file = new TaskResultFile();
		file.setTaskId(taskId);
		file.setFileType(request.getFileType());
		file.setFileName(request.getFileName());
		file.setStoragePath(request.getStoragePath());
		file.setFileSize(request.getFileSize());
		taskResultFileRepository.save(file);
	}

	public void finishTask(Long taskId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(404, "task not found"));
		taskStatusDomainService.transfer(task, TaskStatusEnum.SUCCESS.name(), "task finished", OperatorTypeEnum.NODE.name(), null);
		taskRepository.update(task);
	}

	public void failTask(Long taskId, String failType, String failMessage) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(404, "task not found"));
		task.setFailType(failType);
		task.setFailMessage(failMessage);
		taskStatusDomainService.transfer(task, TaskStatusEnum.FAILED.name(), failMessage, OperatorTypeEnum.NODE.name(), null);
		taskRepository.update(task);
	}
}

