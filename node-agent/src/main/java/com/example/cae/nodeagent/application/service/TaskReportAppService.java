package com.example.cae.nodeagent.application.service;

import com.example.cae.nodeagent.domain.model.ExecutionResult;
import com.example.cae.nodeagent.infrastructure.client.TaskReportClient;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class TaskReportAppService {
	private final TaskReportClient taskReportClient;

	public TaskReportAppService(TaskReportClient taskReportClient) {
		this.taskReportClient = taskReportClient;
	}

	public void reportRunning(Long taskId, String reason) {
		taskReportClient.reportRunning(taskId, reason);
	}

	public void reportLog(Long taskId, Integer seqNo, String content) {
		taskReportClient.reportLog(taskId, seqNo, content);
	}

	public void reportResultSummary(Long taskId, ExecutionResult result) {
		taskReportClient.reportResultSummary(taskId, result);
	}

	public void reportResultFile(Long taskId, File resultFile) {
		taskReportClient.reportResultFile(taskId, resultFile);
	}

	public void markFinished(Long taskId) {
		taskReportClient.markFinished(taskId);
	}

	public void markFailed(Long taskId, String failType, String failMessage) {
		taskReportClient.markFailed(taskId, failType, failMessage);
	}

	public void dispatchFailed(Long taskId, String failType, String reason, boolean recoverable) {
		taskReportClient.dispatchFailed(taskId, failType, reason, recoverable);
	}
}
