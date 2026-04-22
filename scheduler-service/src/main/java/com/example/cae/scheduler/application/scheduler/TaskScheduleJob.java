package com.example.cae.scheduler.application.scheduler;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskDispatchAckDTO;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.dto.TaskScheduleClaimDTO;
import com.example.cae.common.enums.FailTypeEnum;
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
			boolean dispatchAccepted = false;
			try {
				nodeId = taskScheduleManager.schedule(task);
				TaskScheduleClaimDTO scheduleClaim = taskClient.markTaskScheduled(task.getTaskId(), nodeId);
				taskMarkedScheduled = scheduleClaim != null && Boolean.TRUE.equals(scheduleClaim.getClaimed());
				if (!taskMarkedScheduled) {
					taskScheduleManager.releaseNodeReservation(nodeId, task.getTaskId());
					continue;
				}
				nodeAgentClient.notifyDispatch(nodeId, task);
				dispatchAccepted = true;
				TaskDispatchAckDTO dispatchAck = markTaskDispatchedQuietly(task.getTaskId(), nodeId);
				taskScheduleManager.confirmScheduleSuccess(task.getTaskId(), nodeId, buildDispatchSuccessMessage(dispatchAck));
			} catch (Exception ex) {
				handleScheduleException(task, nodeId, taskMarkedScheduled, dispatchAccepted, ex);
			}
		}
	}

	private void handleScheduleException(TaskDTO task,
										 Long nodeId,
										 boolean taskMarkedScheduled,
										 boolean dispatchAccepted,
										 Exception ex) {
		if (nodeId != null && !taskMarkedScheduled) {
			releaseReservationQuietly(nodeId, task == null ? null : task.getTaskId());
		}
		if (taskMarkedScheduled && !dispatchAccepted && task != null && task.getTaskId() != null) {
			handleDispatchFailureQuietly(task.getTaskId(), nodeId, ex);
			return;
		}
		recordScheduleFailureQuietly(task == null ? null : task.getTaskId(), nodeId, ex);
	}

	private TaskDispatchAckDTO markTaskDispatchedQuietly(Long taskId, Long nodeId) {
		return taskClient.markTaskDispatched(taskId, nodeId);
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
}
