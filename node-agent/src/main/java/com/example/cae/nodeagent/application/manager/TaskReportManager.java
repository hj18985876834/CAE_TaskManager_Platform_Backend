package com.example.cae.nodeagent.application.manager;

import com.example.cae.common.enums.FailTypeEnum;
import com.example.cae.nodeagent.application.service.TaskReportAppService;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import com.example.cae.nodeagent.infrastructure.process.ProcessTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TaskReportManager {
	private static final Logger log = LoggerFactory.getLogger(TaskReportManager.class);
	private final TaskReportAppService taskReportAppService;
	private final TaskRuntimeRegistry taskRuntimeRegistry;
	private final ConcurrentMap<Long, AtomicInteger> logSeqMap = new ConcurrentHashMap<>();

	public TaskReportManager(TaskReportAppService taskReportAppService, TaskRuntimeRegistry taskRuntimeRegistry) {
		this.taskReportAppService = taskReportAppService;
		this.taskRuntimeRegistry = taskRuntimeRegistry;
	}

	public void onTaskAccepted(Long taskId) {
		logSeqMap.put(taskId, new AtomicInteger(1));
	}

	public void reportRunning(ExecutionContext context) {
		taskReportAppService.reportRunning(context.getTaskId(), "node-agent start execute");
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
		if (ex instanceof ProcessTimeoutException) {
			taskReportAppService.markTimeout(context.getTaskId());
			return;
		}
		taskReportAppService.markFailed(context.getTaskId(), FailTypeEnum.RUNTIME_ERROR.name(), ex.getMessage());
	}

	public void reportPreRunFailure(ExecutionContext context, Exception ex) {
		String message = ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
				? "node-agent prepare task failed"
				: ex.getMessage();
		taskReportAppService.dispatchFailed(context.getTaskId(), FailTypeEnum.EXECUTOR_START_ERROR.name(), message, false);
		releaseReservationQuietly(context.getTaskId());
	}

	public void reportCanceled(ExecutionContext context, String reason) {
		log.info("task cancel report ignored in first-version status contract, taskId={}, reason={}",
				context == null ? null : context.getTaskId(),
				reason == null || reason.isBlank() ? "task canceled" : reason);
	}

	public void completeTask(Long taskId) {
		logSeqMap.remove(taskId);
		taskRuntimeRegistry.finish(taskId);
	}

	private void releaseReservationQuietly(Long taskId) {
		try {
			taskReportAppService.releaseReservation(taskId);
		} catch (Exception ex) {
			log.warn("failed to release reservation after pre-run failure, taskId={}", taskId, ex);
		}
	}
}
