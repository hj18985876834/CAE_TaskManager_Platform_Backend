package com.example.cae.nodeagent.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
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
	private static final int RUNNING_REPORT_MAX_ATTEMPTS = 50;
	private static final long RUNNING_REPORT_RETRY_INTERVAL_MS = 100L;
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
		BizException lastException = null;
		for (int attempt = 1; attempt <= RUNNING_REPORT_MAX_ATTEMPTS; attempt++) {
			try {
				taskReportAppService.reportRunning(context.getTaskId(), "node-agent start execute");
				return;
			} catch (BizException ex) {
				lastException = ex;
				if (!shouldRetryRunningReport(ex) || attempt == RUNNING_REPORT_MAX_ATTEMPTS) {
					throw ex;
				}
				sleepBeforeRetry(context.getTaskId(), attempt, ex);
			}
		}
		if (lastException != null) {
			throw lastException;
		}
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

	private boolean shouldRetryRunningReport(BizException ex) {
		if (ex == null || ex.getCode() == null) {
			return false;
		}
		return ex.getCode() == ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL
				|| ex.getCode() == ErrorCodeConstants.TASK_STATUS_MISMATCH
				|| ex.getCode() == ErrorCodeConstants.CONFLICT;
	}

	private void sleepBeforeRetry(Long taskId, int attempt, BizException ex) {
		try {
			Thread.sleep(RUNNING_REPORT_RETRY_INTERVAL_MS);
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			throw new BizException(
					ErrorCodeConstants.BAD_GATEWAY,
					"running report retry interrupted, taskId=" + taskId
			);
		}
		log.debug("retry running report, taskId={}, attempt={}, reason={}", taskId, attempt, ex.getMessage());
	}
}
