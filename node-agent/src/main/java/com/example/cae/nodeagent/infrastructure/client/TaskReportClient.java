package com.example.cae.nodeagent.infrastructure.client;

import com.example.cae.nodeagent.domain.model.ExecutionResult;

import java.io.File;

public interface TaskReportClient {
	void reportRunning(Long taskId, String reason);

	void reportLog(Long taskId, Integer seqNo, String content);

	void reportResultSummary(Long taskId, ExecutionResult result);

	void reportResultFile(Long taskId, File file);

	void markFinished(Long taskId);

	void markFailed(Long taskId, String failType, String failMessage);

	void dispatchFailed(Long taskId, String failType, String reason, boolean recoverable);
}
