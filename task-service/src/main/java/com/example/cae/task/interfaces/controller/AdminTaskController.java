package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskCommandAppService;
import com.example.cae.task.application.service.TaskQueryAppService;
import com.example.cae.task.interfaces.request.RetryTaskRequest;
import com.example.cae.task.interfaces.request.TaskListQueryRequest;
import com.example.cae.task.interfaces.request.UpdateTaskPriorityRequest;
import com.example.cae.task.interfaces.response.AdminTaskListItemResponse;
import com.example.cae.task.interfaces.response.TaskSubmitResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {
	private final TaskCommandAppService taskCommandAppService;
	private final TaskQueryAppService taskQueryAppService;

	public AdminTaskController(TaskCommandAppService taskCommandAppService, TaskQueryAppService taskQueryAppService) {
		this.taskCommandAppService = taskCommandAppService;
		this.taskQueryAppService = taskQueryAppService;
	}

	@GetMapping
	public Result<PageResult<AdminTaskListItemResponse>> pageAdminTasks(@Valid TaskListQueryRequest request) {
		return Result.success(taskQueryAppService.pageAdminTasks(request));
	}

	@PostMapping("/{taskId}/priority")
	public Result<Void> adjustPriority(@PathVariable("taskId") @Positive(message = "taskId must be greater than 0") Long taskId,
									   @Valid @RequestBody UpdateTaskPriorityRequest request,
									   @RequestHeader("X-User-Id") @Positive(message = "X-User-Id must be greater than 0") Long adminUserId) {
		taskCommandAppService.adjustPriority(taskId, request, adminUserId);
		return Result.success();
	}

	@PostMapping("/{taskId}/retry")
	public Result<TaskSubmitResponse> retryTask(@PathVariable("taskId") @Positive(message = "taskId must be greater than 0") Long taskId,
												@Valid @RequestBody(required = false) RetryTaskRequest request,
												@RequestHeader("X-User-Id") @Positive(message = "X-User-Id must be greater than 0") Long adminUserId) {
		return Result.success(taskCommandAppService.retryTask(taskId, adminUserId, request == null ? null : request.getReason()));
	}
}
