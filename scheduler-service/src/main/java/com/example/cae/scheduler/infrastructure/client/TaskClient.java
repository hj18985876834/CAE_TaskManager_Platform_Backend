package com.example.cae.scheduler.infrastructure.client;

import com.example.cae.common.dto.TaskBasicDTO;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.dto.TaskDispatchAckDTO;
import com.example.cae.common.dto.TaskScheduleClaimDTO;
import com.example.cae.common.dto.TaskStatusAckDTO;

import java.util.Map;
import java.util.List;

public interface TaskClient {
	List<TaskDTO> listPendingTasks(Integer limit);

	Map<Long, TaskBasicDTO> getTaskBasics(List<Long> taskIds);

	TaskScheduleClaimDTO markTaskScheduled(Long taskId, Long nodeId);

	TaskDispatchAckDTO markTaskDispatched(Long taskId, Long nodeId);

	TaskStatusAckDTO markTaskFailed(Long taskId, Long nodeId, String failType, String reason, boolean recoverable);

	int markNodeOfflineTasksFailed(Long nodeId, String reason);
}
