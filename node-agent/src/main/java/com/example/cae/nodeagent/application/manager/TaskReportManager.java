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
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TaskReportManager {
	private static final Logger log = LoggerFactory.getLogger(TaskReportManager.class);
	private static final int RUNNING_REPORT_MAX_ATTEMPTS = 50;
	private static final long RUNNING_REPORT_RETRY_INTERVAL_MS = 100L;
	private static final int TERMINAL_REPORT_MAX_ATTEMPTS = 10;
	private static final long TERMINAL_REPORT_RETRY_INTERVAL_MS = 200L;
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
		Exception lastException = null;
		for (int attempt = 1; attempt <= RUNNING_REPORT_MAX_ATTEMPTS; attempt++) {
			try {
				taskReportAppService.reportRunning(context.getTaskId(), "node-agent start execute");
				return;
			} catch (Exception ex) {
				lastException = ex;
				if (!shouldRetryRunningReport(ex) || attempt == RUNNING_REPORT_MAX_ATTEMPTS) {
					throw propagateRunningReportException(ex);
				}
				sleepBeforeRetry(context.getTaskId(), attempt, ex);
			}
		}
		if (lastException != null) {
			throw propagateRunningReportException(lastException);
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
		retryTerminalReport(context == null ? null : context.getTaskId(), "report success", () -> {
			taskReportAppService.reportResultSummary(context.getTaskId(), result);
			if (result.getResultFiles() != null) {
				for (File file : result.getResultFiles()) {
					taskReportAppService.reportResultFile(context.getTaskId(), file);
				}
			}
			taskReportAppService.markFinished(context.getTaskId());
		});
	}

	public void reportFail(ExecutionContext context, Exception ex) {
		if (ex instanceof ProcessTimeoutException) {
			retryTerminalReport(context == null ? null : context.getTaskId(), "mark timeout",
					() -> taskReportAppService.markTimeout(context.getTaskId()));
			return;
		}
		retryTerminalReport(context == null ? null : context.getTaskId(), "mark failed",
				() -> taskReportAppService.markFailed(context.getTaskId(), FailTypeEnum.RUNTIME_ERROR.name(), ex.getMessage()));
	}

	public void reportPreRunFailure(ExecutionContext context, Exception ex) {
		String message = ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
				? "node-agent prepare task failed"
				: ex.getMessage();
		if (isRecoverablePreRunFailure(ex)) {
			taskReportAppService.dispatchFailed(
					context.getTaskId(),
					FailTypeEnum.DISPATCH_ERROR.name(),
					message,
					true
			);
			return;
		}
		taskReportAppService.dispatchFailed(
				context.getTaskId(),
				FailTypeEnum.EXECUTOR_START_ERROR.name(),
				message,
				false
		);
	}

	public void reportCanceled(ExecutionContext context, String reason) {
		log.info("task cancel report ignored in first-version status contract, taskId={}, reason={}",
				context == null ? null : context.getTaskId(),
				reason == null || reason.isBlank() ? "task canceled" : reason);
	}

	public void reportPostSuccessCallbackFailure(ExecutionContext context, Exception ex) {
		log.error("task finished successfully but callback reporting did not complete, taskId={}, message={}",
				context == null ? null : context.getTaskId(),
				ex == null ? null : ex.getMessage(),
				ex);
	}

	public void completeTask(Long taskId) {
		logSeqMap.remove(taskId);
		taskRuntimeRegistry.finish(taskId);
	}

	private boolean shouldRetryRunningReport(Exception ex) {
		if (ex instanceof RestClientException) {
			return true;
		}
		if (!(ex instanceof BizException bizException) || bizException.getCode() == null) {
			return false;
		}
		return bizException.getCode() == ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL
				|| bizException.getCode() == ErrorCodeConstants.TASK_STATUS_MISMATCH
				|| bizException.getCode() == ErrorCodeConstants.CONFLICT
				|| bizException.getCode() == ErrorCodeConstants.BAD_GATEWAY;
	}

	private boolean isRecoverablePreRunFailure(Exception ex) {
		return shouldRetryRunningReport(ex);
	}

	private void retryTerminalReport(Long taskId, String action, Runnable runnable) {
		Exception lastException = null;
		for (int attempt = 1; attempt <= TERMINAL_REPORT_MAX_ATTEMPTS; attempt++) {
			try {
				runnable.run();
				return;
			} catch (Exception ex) {
				lastException = ex;
				if (!shouldRetryTerminalReport(ex) || attempt == TERMINAL_REPORT_MAX_ATTEMPTS) {
					throw propagateRunningReportException(ex);
				}
				sleepBeforeRetry(taskId, attempt, ex, TERMINAL_REPORT_RETRY_INTERVAL_MS, action);
			}
		}
		if (lastException != null) {
			throw propagateRunningReportException(lastException);
		}
	}

	private RuntimeException propagateRunningReportException(Exception ex) {
		if (ex instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		return new BizException(ErrorCodeConstants.BAD_GATEWAY, "running report failed", ex);
	}

	private void sleepBeforeRetry(Long taskId, int attempt, Exception ex) {
		sleepBeforeRetry(taskId, attempt, ex, RUNNING_REPORT_RETRY_INTERVAL_MS, "running report");
	}

	private boolean shouldRetryTerminalReport(Exception ex) {
		if (ex instanceof RestClientException) {
			return true;
		}
		if (!(ex instanceof BizException bizException) || bizException.getCode() == null) {
			return false;
		}
		return bizException.getCode() == ErrorCodeConstants.BAD_GATEWAY
				|| bizException.getCode() == ErrorCodeConstants.CONFLICT;
	}

	private void sleepBeforeRetry(Long taskId, int attempt, Exception ex, long intervalMs, String action) {
		try {
			Thread.sleep(intervalMs);
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			throw new BizException(
					ErrorCodeConstants.BAD_GATEWAY,
					action + " retry interrupted, taskId=" + taskId
			);
		}
		log.debug("retry {}, taskId={}, attempt={}, reason={}", action, taskId, attempt, ex.getMessage());
	}
}
