package com.example.cae.task.application.scheduler;

import com.example.cae.task.application.manager.TaskLifecycleManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StaleUnsubmittedTaskCleanupJob {
	private final TaskLifecycleManager taskLifecycleManager;

	public StaleUnsubmittedTaskCleanupJob(TaskLifecycleManager taskLifecycleManager) {
		this.taskLifecycleManager = taskLifecycleManager;
	}

	@Scheduled(fixedDelayString = "${task.cleanup.stale-unsubmitted-interval-ms:3600000}")
	public void run() {
		taskLifecycleManager.cleanStaleUnsubmittedTasks();
	}
}
