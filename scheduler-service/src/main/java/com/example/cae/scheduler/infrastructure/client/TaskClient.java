package com.example.cae.scheduler.infrastructure.client;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.dto.TaskDispatchAckDTO;
import com.example.cae.common.dto.TaskScheduleClaimDTO;

import java.util.List;

public interface TaskClient {
	List<TaskDTO> listPendingTasks(Integer limit);

	TaskScheduleClaimDTO markTaskScheduled(Long taskId, Long nodeId);

	TaskDispatchAckDTO markTaskDispatched(Long taskId, Long nodeId);

	void markTaskFailed(Long taskId, Long nodeId, String failType, String reason, boolean recoverable);

	int markNodeOfflineTasksFailed(Long nodeId, String reason);
}
