package com.example.cae.scheduler.application.manager;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.dto.TaskStatusAckDTO;
import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.service.ScheduleAppService;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;
import com.example.cae.scheduler.interfaces.response.ScheduleRecordResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskScheduleManager {
	private final ScheduleAppService scheduleAppService;

	public TaskScheduleManager(ScheduleAppService scheduleAppService) {
		this.scheduleAppService = scheduleAppService;
	}

	public Long schedule(TaskDTO task) {
		return scheduleAppService.scheduleTask(task);
	}

	public void confirmScheduleSuccess(Long taskId, Long nodeId, String message) {
		scheduleAppService.confirmScheduleSuccess(taskId, nodeId, message);
	}

	public void recordScheduleFailure(Long taskId, Long nodeId, String message) {
		scheduleAppService.recordScheduleFailure(taskId, nodeId, message);
	}

	public void releaseNodeReservation(Long nodeId, Long taskId) {
		scheduleAppService.releaseNodeReservation(nodeId, taskId);
	}

	public TaskStatusAckDTO handleDispatchFailure(Long taskId, Long nodeId, String failType, String reason, boolean recoverable) {
		return scheduleAppService.handleDispatchFailure(taskId, nodeId, failType, reason, recoverable);
	}

	public void cancelTaskOnNode(Long nodeId, Long taskId, String reason) {
		scheduleAppService.cancelTaskOnNode(nodeId, taskId, reason);
	}

	public PageResult<ScheduleRecordResponse> pageRecords(SchedulePageQueryRequest request) {
		return scheduleAppService.pageRecords(request);
	}

	public List<ScheduleRecordResponse> listByTaskId(Long taskId) {
		return scheduleAppService.listByTaskId(taskId);
	}
}
