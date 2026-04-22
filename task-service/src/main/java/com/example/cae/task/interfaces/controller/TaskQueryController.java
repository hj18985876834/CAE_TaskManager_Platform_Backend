package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskQueryAppService;
import com.example.cae.task.interfaces.request.TaskListQueryRequest;
import com.example.cae.task.interfaces.response.TaskDetailResponse;
import com.example.cae.task.interfaces.response.TaskFileResponse;
import com.example.cae.task.interfaces.response.TaskListItemResponse;
import com.example.cae.task.interfaces.response.TaskStatusHistoryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/tasks")
public class TaskQueryController {
	private final TaskQueryAppService taskQueryAppService;

	public TaskQueryController(TaskQueryAppService taskQueryAppService) {
		this.taskQueryAppService = taskQueryAppService;
	}

	@GetMapping
	public Result<PageResult<TaskListItemResponse>> pageMyTasks(@Valid TaskListQueryRequest request, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId) {
		return Result.success(taskQueryAppService.pageMyTasks(request, userId));
	}

	@GetMapping("/{taskId}")
	public Result<TaskDetailResponse> getTaskDetail(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId, @RequestHeader(value = "X-Role-Code", required = false) String roleCode) {
		return Result.success(taskQueryAppService.getTaskDetail(taskId, userId, roleCode));
	}

	@GetMapping("/{taskId}/status-history")
	public Result<List<TaskStatusHistoryResponse>> getTaskStatusHistory(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId, @RequestHeader(value = "X-Role-Code", required = false) String roleCode) {
		return Result.success(taskQueryAppService.getTaskStatusHistory(taskId, userId, roleCode));
	}

	@GetMapping("/{taskId}/files")
	public Result<List<TaskFileResponse>> getTaskFiles(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId, @RequestHeader(value = "X-Role-Code", required = false) String roleCode) {
		return Result.success(taskQueryAppService.getTaskFiles(taskId, userId, roleCode));
	}
}
