package com.example.cae.scheduler.application.scheduler;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.dto.TaskScheduleClaimDTO;
import com.example.cae.common.enums.FailTypeEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.scheduler.application.manager.TaskScheduleManager;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Set;

@Component
public class TaskScheduleJob {
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
				markTaskDispatchedQuietly(task.getTaskId(), nodeId);
				nodeAgentClient.notifyDispatch(nodeId, task);
				dispatchAccepted = true;
				taskScheduleManager.confirmScheduleSuccess(task.getTaskId(), nodeId, "task dispatched");
			} catch (Exception ex) {
				if (nodeId != null && !dispatchAccepted) {
					taskScheduleManager.releaseNodeReservation(nodeId, task == null ? null : task.getTaskId());
				}
				if (taskMarkedScheduled && !dispatchAccepted && task != null && task.getTaskId() != null) {
					String currentStatus = taskClient.getTaskStatus(task.getTaskId());
					boolean alreadyAdvanced = currentStatus != null
							&& Set.of("RUNNING", "SUCCESS", "FAILED", "CANCELED", "TIMEOUT").contains(currentStatus);
					if (!alreadyAdvanced) {
						taskClient.markTaskFailed(
								task.getTaskId(),
								nodeId,
								FailTypeEnum.DISPATCH_ERROR.name(),
								ex.getMessage() == null || ex.getMessage().isBlank() ? "task dispatch failed" : ex.getMessage(),
								isRecoverableDispatchError(ex)
						);
					}
				}
				taskScheduleManager.recordScheduleFailure(task == null ? null : task.getTaskId(), nodeId, ex.getMessage());
			}
		}
	}

	private void markTaskDispatchedQuietly(Long taskId, Long nodeId) {
		taskClient.markTaskDispatched(taskId, nodeId);
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
