package com.example.cae.scheduler.application.scheduler;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskDispatchAckDTO;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.dto.TaskScheduleClaimDTO;
import com.example.cae.common.enums.FailTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.scheduler.application.manager.TaskScheduleManager;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class TaskScheduleJob {
	private static final Logger log = LoggerFactory.getLogger(TaskScheduleJob.class);
	private static final int TASK_CONFIRM_MAX_ATTEMPTS = 5;
	private static final long TASK_CONFIRM_RETRY_INTERVAL_MS = 100L;
	private final TaskClient taskClient;
	private final NodeAgentClient nodeAgentClient;
	private final TaskScheduleManager taskScheduleManager;

	public TaskScheduleJob(TaskClient taskClient, NodeAgentClient nodeAgentClient, TaskScheduleManager taskScheduleManager) {
		this.taskClient = taskClient;
		this.nodeAgentClient = nodeAgentClient;
		this.taskScheduleManager = taskScheduleManager;
	}

	@Scheduled(fixedDelayString = "${scheduler.task-schedule-interval-ms:5000}")
	public void run() {
		List<TaskDTO> pendingTasks = taskClient.listPendingTasks(20);
		for (TaskDTO task : pendingTasks) {
			Long nodeId = null;
			boolean taskMarkedScheduled = false;
			boolean nodeAccepted = false;
			try {
				nodeId = taskScheduleManager.schedule(task);
				TaskScheduleClaimDTO scheduleClaim = markTaskScheduledWithRetry(task.getTaskId(), nodeId);
				taskMarkedScheduled = shouldContinueDispatchAfterScheduleClaim(scheduleClaim, nodeId);
				if (!taskMarkedScheduled) {
					if (shouldReleaseReservationAfterRejectedClaim(scheduleClaim, nodeId)) {
						taskScheduleManager.releaseNodeReservation(nodeId, task.getTaskId());
					}
					continue;
				}
				nodeAgentClient.notifyDispatch(nodeId, task);
				nodeAccepted = true;
				TaskDispatchAckDTO dispatchAck = markTaskDispatchedWithRetry(task.getTaskId(), nodeId);
				taskScheduleManager.confirmScheduleSuccess(task.getTaskId(), nodeId, buildDispatchSuccessMessage(dispatchAck));
			} catch (Exception ex) {
				handleScheduleException(task, nodeId, taskMarkedScheduled, nodeAccepted, ex);
			}
		}
	}

	private void handleScheduleException(TaskDTO task,
										 Long nodeId,
										 boolean taskMarkedScheduled,
										 boolean nodeAccepted,
										 Exception ex) {
		if (nodeId != null && !taskMarkedScheduled) {
			releaseReservationQuietly(nodeId, task == null ? null : task.getTaskId());
		}
		if (taskMarkedScheduled && task != null && task.getTaskId() != null) {
			if (nodeAccepted) {
				recordScheduleFailureQuietly(task.getTaskId(), nodeId, ex);
				return;
			}
			handleDispatchFailureQuietly(task.getTaskId(), nodeId, ex);
			return;
		}
		recordScheduleFailureQuietly(task == null ? null : task.getTaskId(), nodeId, ex);
	}

	private TaskScheduleClaimDTO markTaskScheduledWithRetry(Long taskId, Long nodeId) {
		Exception lastException = null;
		for (int attempt = 1; attempt <= TASK_CONFIRM_MAX_ATTEMPTS; attempt++) {
			try {
				return taskClient.markTaskScheduled(taskId, nodeId);
			} catch (Exception ex) {
				lastException = ex;
				if (!shouldRetryTaskStateConfirm(ex) || attempt == TASK_CONFIRM_MAX_ATTEMPTS) {
					throw ex;
				}
				sleepBeforeRetry("mark-scheduled", taskId, nodeId, attempt, ex);
			}
		}
		if (lastException instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "mark scheduled failed");
	}

	private TaskDispatchAckDTO markTaskDispatchedWithRetry(Long taskId, Long nodeId) {
		Exception lastException = null;
		for (int attempt = 1; attempt <= TASK_CONFIRM_MAX_ATTEMPTS; attempt++) {
			try {
				return taskClient.markTaskDispatched(taskId, nodeId);
			} catch (Exception ex) {
				lastException = ex;
				if (!shouldRetryTaskStateConfirm(ex) || attempt == TASK_CONFIRM_MAX_ATTEMPTS) {
					throw ex;
				}
				sleepBeforeRetry("mark-dispatched", taskId, nodeId, attempt, ex);
			}
		}
		if (lastException instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "mark dispatched failed");
	}

	private void releaseReservationQuietly(Long nodeId, Long taskId) {
		try {
			taskScheduleManager.releaseNodeReservation(nodeId, taskId);
		} catch (Exception releaseEx) {
			recordScheduleFailureQuietly(taskId, nodeId, releaseEx);
		}
	}

	private void handleDispatchFailureQuietly(Long taskId, Long nodeId, Exception ex) {
		try {
			taskScheduleManager.handleDispatchFailure(
					taskId,
					nodeId,
					FailTypeEnum.DISPATCH_ERROR.name(),
					ex == null || ex.getMessage() == null || ex.getMessage().isBlank() ? "task dispatch failed" : ex.getMessage(),
					isRecoverableDispatchError(ex)
			);
		} catch (Exception callbackEx) {
			recordScheduleFailureQuietly(taskId, nodeId, callbackEx);
		}
	}

	private void recordScheduleFailureQuietly(Long taskId, Long nodeId, Exception ex) {
		try {
			taskScheduleManager.recordScheduleFailure(taskId, nodeId, ex == null ? null : ex.getMessage());
		} catch (Exception recordEx) {
			log.warn("failed to record schedule failure, taskId={}, nodeId={}", taskId, nodeId, recordEx);
		}
	}

	private String buildDispatchSuccessMessage(TaskDispatchAckDTO dispatchAck) {
		if (dispatchAck == null || dispatchAck.getStatus() == null || dispatchAck.getStatus().isBlank()) {
			return "task dispatched";
		}
		if ("RUNNING".equalsIgnoreCase(dispatchAck.getStatus())) {
			return "task dispatched, already running";
		}
		return "task dispatched";
	}

	private boolean isRecoverableDispatchError(Exception ex) {
		if (ex instanceof BizException bizException) {
			return bizException.getCode() == ErrorCodeConstants.CONFLICT
					|| bizException.getCode() == ErrorCodeConstants.BAD_GATEWAY
					|| bizException.getCode() == ErrorCodeConstants.NODE_AGENT_EMPTY_RESPONSE
					|| bizException.getCode() == ErrorCodeConstants.NODE_AGENT_REJECTED;
		}
		return ex instanceof RestClientException;
	}

	private boolean shouldRetryTaskStateConfirm(Exception ex) {
		if (ex instanceof RestClientException) {
			return true;
		}
		if (ex instanceof BizException bizException && bizException.getCode() != null) {
			return bizException.getCode() == ErrorCodeConstants.BAD_GATEWAY
					|| bizException.getCode() == ErrorCodeConstants.NODE_AGENT_EMPTY_RESPONSE;
		}
		return false;
	}

	private boolean shouldContinueDispatchAfterScheduleClaim(TaskScheduleClaimDTO scheduleClaim, Long expectedNodeId) {
		if (scheduleClaim == null) {
			return false;
		}
		if (Boolean.TRUE.equals(scheduleClaim.getClaimed())) {
			return true;
		}
		return isSameNodeClaim(scheduleClaim, expectedNodeId)
				&& TaskStatusEnum.SCHEDULED.name().equals(scheduleClaim.getStatus());
	}

	private boolean shouldReleaseReservationAfterRejectedClaim(TaskScheduleClaimDTO scheduleClaim, Long expectedNodeId) {
		if (scheduleClaim == null || expectedNodeId == null) {
			return true;
		}
		if (isSameNodeClaim(scheduleClaim, expectedNodeId)
				&& TaskStatusEnum.DISPATCHED.name().equals(scheduleClaim.getStatus())) {
			return false;
		}
		return true;
	}

	private boolean isSameNodeClaim(TaskScheduleClaimDTO scheduleClaim, Long expectedNodeId) {
		return scheduleClaim != null
				&& expectedNodeId != null
				&& expectedNodeId.equals(scheduleClaim.getNodeId());
	}

	private void sleepBeforeRetry(String action, Long taskId, Long nodeId, int attempt, Exception ex) {
		try {
			Thread.sleep(TASK_CONFIRM_RETRY_INTERVAL_MS);
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY,
					action + " retry interrupted, taskId=" + taskId + ", nodeId=" + nodeId);
		}
		log.debug("retry {}, taskId={}, nodeId={}, attempt={}, reason={}",
				action, taskId, nodeId, attempt, ex == null ? null : ex.getMessage());
	}
}
