package com.example.cae.scheduler.infrastructure.client;

import com.example.cae.common.dto.TaskDTO;

import java.util.List;

public interface TaskClient {
	List<TaskDTO> listPendingTasks();

	void markTaskScheduled(Long taskId, Long nodeId);

	void markTaskDispatched(Long taskId, Long nodeId);
}

