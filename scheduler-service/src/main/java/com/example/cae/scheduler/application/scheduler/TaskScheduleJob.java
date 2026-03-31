package com.example.cae.scheduler.application.scheduler;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.scheduler.application.manager.TaskScheduleManager;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

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
			try {
				nodeId = taskScheduleManager.schedule(task);
				taskClient.markTaskScheduled(task.getTaskId(), nodeId);
				nodeAgentClient.notifyDispatch(nodeId, task);
				taskClient.markTaskDispatched(task.getTaskId(), nodeId);
				taskScheduleManager.confirmScheduleSuccess(task.getTaskId(), nodeId, "task dispatched");
			} catch (Exception ex) {
				if (nodeId != null) {
					taskScheduleManager.releaseNodeReservation(nodeId);
				}
				taskScheduleManager.recordScheduleFailure(task == null ? null : task.getTaskId(), nodeId, ex.getMessage());
			}
		}
	}
}
