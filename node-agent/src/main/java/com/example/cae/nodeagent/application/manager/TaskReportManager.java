package com.example.cae.nodeagent.application.manager;

import com.example.cae.common.enums.FailTypeEnum;
import com.example.cae.nodeagent.application.service.TaskReportAppService;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import com.example.cae.nodeagent.infrastructure.process.ProcessTimeoutException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TaskReportManager {
	private final TaskReportAppService taskReportAppService;
	private final TaskRuntimeRegistry taskRuntimeRegistry;
	private final ConcurrentMap<Long, AtomicInteger> logSeqMap = new ConcurrentHashMap<>();

	public TaskReportManager(TaskReportAppService taskReportAppService, TaskRuntimeRegistry taskRuntimeRegistry) {
		this.taskReportAppService = taskReportAppService;
		this.taskRuntimeRegistry = taskRuntimeRegistry;
	}

	public void onTaskAccepted(Long taskId) {
		logSeqMap.put(taskId, new AtomicInteger(1));
		taskReportAppService.reportStatus(taskId, "DISPATCHED", "node-agent accepted dispatch");
	}

	public void reportRunning(ExecutionContext context) {
		taskReportAppService.reportStatus(context.getTaskId(), "RUNNING", "node-agent start execute");
	}

	public void pushLog(Long taskId, Integer seqNo, String line) {
		int actualSeqNo = seqNo == null
				? logSeqMap.computeIfAbsent(taskId, key -> new AtomicInteger(1)).getAndIncrement()
				: seqNo;
		taskReportAppService.reportLog(taskId, actualSeqNo, line);
	}

	public void reportSuccess(ExecutionContext context, ExecutionResult result, long startMillis) {
		int duration = result.getDurationSeconds() == null
				? (int) ((System.currentTimeMillis() - startMillis) / 1000)
				: result.getDurationSeconds();
		result.setDurationSeconds(duration);
		taskReportAppService.reportResultSummary(context.getTaskId(), result);
		if (result.getResultFiles() != null) {
			for (File file : result.getResultFiles()) {
				taskReportAppService.reportResultFile(context.getTaskId(), file);
			}
		}
		taskReportAppService.markFinished(context.getTaskId());
	}

	public void reportFail(ExecutionContext context, Exception ex) {
		String failType = ex instanceof ProcessTimeoutException ? FailTypeEnum.TIMEOUT.name() : FailTypeEnum.RUNTIME_ERROR.name();
		taskReportAppService.markFailed(context.getTaskId(), failType, ex.getMessage());
	}

	public void reportPreRunFailure(ExecutionContext context, Exception ex) {
		String message = ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
				? "node-agent prepare task failed"
				: ex.getMessage();
		taskReportAppService.dispatchFailed(context.getTaskId(), FailTypeEnum.EXECUTOR_START_ERROR.name(), message, false);
	}

	public void reportCanceled(ExecutionContext context, String reason) {
		String message = reason == null || reason.isBlank() ? "task canceled" : reason;
		taskReportAppService.reportStatus(context.getTaskId(), "CANCELED", message);
	}

	public void completeTask(Long taskId) {
		logSeqMap.remove(taskId);
		taskRuntimeRegistry.finish(taskId);
	}
}
