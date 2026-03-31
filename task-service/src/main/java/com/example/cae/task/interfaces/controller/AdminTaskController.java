package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskQueryAppService;
import com.example.cae.task.interfaces.request.TaskListQueryRequest;
import com.example.cae.task.interfaces.response.TaskListItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {
	private final TaskQueryAppService taskQueryAppService;

	public AdminTaskController(TaskQueryAppService taskQueryAppService) {
		this.taskQueryAppService = taskQueryAppService;
	}

	@GetMapping
	public Result<PageResult<TaskListItemResponse>> pageAdminTasks(TaskListQueryRequest request) {
		return Result.success(taskQueryAppService.pageAdminTasks(request));
	}
}
