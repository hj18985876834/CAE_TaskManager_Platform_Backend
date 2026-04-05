package com.example.cae.scheduler.infrastructure.client;

import com.example.cae.common.dto.TaskDTO;

import java.util.List;

public interface TaskClient {
	List<TaskDTO> listPendingTasks(Integer limit);

	void markTaskScheduled(Long taskId, Long nodeId);

	void markTaskDispatched(Long taskId, Long nodeId);

	void markTaskFailed(Long taskId, String failType, String reason);

	void markNodeOfflineTasksFailed(Long nodeId, String reason);
}
