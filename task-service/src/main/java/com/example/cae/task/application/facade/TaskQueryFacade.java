package com.example.cae.task.application.facade;

import com.example.cae.common.response.PageResult;
import com.example.cae.task.application.service.TaskLogAppService;
import com.example.cae.task.application.service.TaskQueryAppService;
import com.example.cae.task.application.service.TaskResultAppService;
import com.example.cae.task.interfaces.request.AdminTaskListQueryRequest;
import com.example.cae.task.interfaces.request.MyTaskListQueryRequest;
import com.example.cae.task.interfaces.response.AdminTaskListItemResponse;
import com.example.cae.task.interfaces.response.TaskDetailResponse;
import com.example.cae.task.interfaces.response.TaskInputFileResponse;
import com.example.cae.task.interfaces.response.TaskListItemResponse;
import com.example.cae.task.interfaces.response.TaskLogPageResponse;
import com.example.cae.task.interfaces.response.TaskLogResponse;
import com.example.cae.task.interfaces.response.TaskResultFileResponse;
import com.example.cae.task.interfaces.response.TaskResultSummaryResponse;
import com.example.cae.task.interfaces.response.TaskScheduleRecordResponse;
import com.example.cae.task.interfaces.response.TaskStatusHistoryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskQueryFacade {
	private final TaskQueryAppService taskQueryAppService;
	private final TaskLogAppService taskLogAppService;
	private final TaskResultAppService taskResultAppService;

	public TaskQueryFacade(TaskQueryAppService taskQueryAppService, TaskLogAppService taskLogAppService, TaskResultAppService taskResultAppService) {
		this.taskQueryAppService = taskQueryAppService;
		this.taskLogAppService = taskLogAppService;
		this.taskResultAppService = taskResultAppService;
	}

	public PageResult<TaskListItemResponse> pageMyTasks(MyTaskListQueryRequest request, Long userId) {
		return taskQueryAppService.pageMyTasks(request, userId);
	}

	public PageResult<AdminTaskListItemResponse> pageAdminTasks(AdminTaskListQueryRequest request) {
		return taskQueryAppService.pageAdminTasks(request);
	}

	public TaskDetailResponse getTaskDetail(Long taskId, Long userId, String roleCode) {
		return taskQueryAppService.getTaskDetail(taskId, userId, roleCode);
	}

	public List<TaskStatusHistoryResponse> getTaskStatusHistory(Long taskId, Long userId, String roleCode) {
		return taskQueryAppService.getTaskStatusHistory(taskId, userId, roleCode);
	}

	public List<TaskInputFileResponse> getTaskFiles(Long taskId, Long userId, String roleCode) {
		return taskQueryAppService.getTaskFiles(taskId, userId, roleCode);
	}

	public List<TaskScheduleRecordResponse> getTaskScheduleRecords(Long taskId, Long userId, String roleCode) {
		return taskQueryAppService.getTaskScheduleRecords(taskId, userId, roleCode);
	}

	public TaskLogPageResponse getLogs(Long taskId, Integer fromSeq, Integer pageSize, Long userId, String roleCode) {
		return taskLogAppService.getLogs(taskId, fromSeq, pageSize, userId, roleCode);
	}

	public TaskResultSummaryResponse getResultSummary(Long taskId, Long userId, String roleCode) {
		return taskResultAppService.getResultSummary(taskId, userId, roleCode);
	}

	public List<TaskResultFileResponse> getResultFiles(Long taskId, Long userId, String roleCode) {
		return taskResultAppService.getResultFiles(taskId, userId, roleCode);
	}
}
