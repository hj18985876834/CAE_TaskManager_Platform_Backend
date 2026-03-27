package com.example.cae.scheduler.application.facade;

import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.manager.TaskScheduleManager;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;
import com.example.cae.scheduler.interfaces.response.ScheduleRecordResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduleFacade {
	private final TaskScheduleManager taskScheduleManager;

	public ScheduleFacade(TaskScheduleManager taskScheduleManager) {
		this.taskScheduleManager = taskScheduleManager;
	}

	public PageResult<ScheduleRecordResponse> pageRecords(SchedulePageQueryRequest request) {
		return taskScheduleManager.pageRecords(request);
	}

	public List<ScheduleRecordResponse> listByTaskId(Long taskId) {
		return taskScheduleManager.listByTaskId(taskId);
	}
}
