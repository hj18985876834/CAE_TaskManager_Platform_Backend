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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.file.Path;
import java.util.HexFormat;
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
	private static final int DISPATCH_FAILURE_REPORT_MAX_ATTEMPTS = 10;
	private static final long DISPATCH_FAILURE_REPORT_RETRY_INTERVAL_MS = 200L;
	private static final int MAX_RESULT_FILE_NAME_LENGTH = 255;
	private static final int RESULT_FILE_HASH_LENGTH = 16;
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
					taskReportAppService.reportResultFile(context.getTaskId(), file, buildResultFileName(context, file));
				}
			}
			taskReportAppService.markFinished(context.getTaskId());
		});
	}

	private String buildResultFileName(ExecutionContext context, File file) {
		if (file == null) {
			return null;
		}
		String fallbackName = file.getName();
		if (context == null || context.getOutputDir() == null || context.getOutputDir().isBlank()) {
			return fallbackName;
		}
		try {
			Path outputDir = Path.of(context.getOutputDir()).toAbsolutePath().normalize();
			Path filePath = file.toPath().toAbsolutePath().normalize();
			if (!filePath.startsWith(outputDir)) {
				return fallbackName;
			}
			String relativeName = outputDir.relativize(filePath).toString().replace('\\', '/');
			return sanitizeResultFileName(relativeName);
		} catch (Exception ex) {
			return fallbackName;
		}
	}

	private String sanitizeResultFileName(String relativeName) {
		if (relativeName == null || relativeName.isBlank()) {
			return relativeName;
		}
		String sanitized = relativeName.replace("/", "__").replace("\\", "__");
		sanitized = replaceControlCharacters(sanitized);
		if (sanitized.length() <= MAX_RESULT_FILE_NAME_LENGTH) {
			return sanitized;
		}
		String hash = shortHash(relativeName);
		int prefixLength = MAX_RESULT_FILE_NAME_LENGTH - hash.length() - 2;
		return sanitized.substring(0, Math.max(1, prefixLength)) + "__" + hash;
	}

	private String replaceControlCharacters(String value) {
		StringBuilder builder = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char current = value.charAt(i);
			builder.append(Character.isISOControl(current) ? '_' : current);
		}
		return builder.toString();
	}

	private String shortHash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(bytes).substring(0, RESULT_FILE_HASH_LENGTH);
		} catch (Exception ex) {
			return Integer.toHexString(value.hashCode());
		}
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
		Long taskId = context == null ? null : context.getTaskId();
		String message = ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
				? "node-agent prepare task failed"
				: ex.getMessage();
		if (isRecoverablePreRunFailure(ex)) {
			retryDispatchFailureReport(
					taskId,
					() -> taskReportAppService.dispatchFailed(
							taskId,
							FailTypeEnum.DISPATCH_ERROR.name(),
							message,
							true
					)
			);
			return;
		}
		retryDispatchFailureReport(
				taskId,
				() -> taskReportAppService.dispatchFailed(
						taskId,
						FailTypeEnum.EXECUTOR_START_ERROR.name(),
						message,
						false
				)
		);
	}

	public void reportCanceled(ExecutionContext context, String reason) {
		log.info("task cancel report ignored in first-version status contract, taskId={}, reason={}",
				context == null ? null : context.getTaskId(),
				reason == null || reason.isBlank() ? "task canceled" : reason);
	}

	public void reportPostSuccessCallbackFailure(ExecutionContext context, Exception ex) {
		Long taskId = context == null ? null : context.getTaskId();
		String message = ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
				? "post-success callback reporting failed"
				: ex.getMessage();
		log.error("task finished successfully but callback reporting did not complete, taskId={}, message={}",
				taskId,
				message,
				ex);
		try {
			retryTerminalReport(taskId, "mark callback failed",
					() -> taskReportAppService.markFailed(taskId, FailTypeEnum.RUNTIME_ERROR.name(), message));
		} catch (Exception reportEx) {
			log.error("failed to report post-success callback failure, taskId={}, message={}",
					taskId,
					reportEx.getMessage(),
					reportEx);
		}
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

	private void retryDispatchFailureReport(Long taskId, Runnable runnable) {
		Exception lastException = null;
		for (int attempt = 1; attempt <= DISPATCH_FAILURE_REPORT_MAX_ATTEMPTS; attempt++) {
			try {
				runnable.run();
				return;
			} catch (Exception ex) {
				lastException = ex;
				if (!shouldRetryDispatchFailureReport(ex) || attempt == DISPATCH_FAILURE_REPORT_MAX_ATTEMPTS) {
					throw propagateRunningReportException(ex);
				}
				sleepBeforeRetry(taskId, attempt, ex, DISPATCH_FAILURE_REPORT_RETRY_INTERVAL_MS, "dispatch failure report");
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

	private boolean shouldRetryDispatchFailureReport(Exception ex) {
		if (ex instanceof RestClientException) {
			return true;
		}
		if (!(ex instanceof BizException bizException) || bizException.getCode() == null) {
			return false;
		}
		return bizException.getCode() == ErrorCodeConstants.BAD_GATEWAY;
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
