package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskCommandAppService;
import com.example.cae.task.interfaces.request.CancelTaskRequest;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
	private final TaskCommandAppService taskCommandAppService;

	public TaskController(TaskCommandAppService taskCommandAppService) {
		this.taskCommandAppService = taskCommandAppService;
	}

	@PostMapping
	public Result<TaskCreateResponse> createTask(@RequestBody CreateTaskRequest request, @RequestHeader("X-User-Id") Long userId) {
		return Result.success(taskCommandAppService.createTask(request, userId));
	}

	@PostMapping("/{taskId}/files")
	public Result<Void> uploadTaskFiles(@PathVariable("taskId") Long taskId, @RequestPart("files") MultipartFile[] files, @RequestHeader("X-User-Id") Long userId) {
		taskCommandAppService.uploadTaskFiles(taskId, files, userId);
		return Result.success();
	}

	@PostMapping("/{taskId}/validate")
	public Result<Void> validateTask(@PathVariable("taskId") Long taskId, @RequestHeader("X-User-Id") Long userId) {
		taskCommandAppService.validateTask(taskId, userId);
		return Result.success();
	}

	@PostMapping("/{taskId}/submit")
	public Result<Void> submitTask(@PathVariable("taskId") Long taskId, @RequestHeader("X-User-Id") Long userId) {
		taskCommandAppService.submitTask(taskId, userId);
		return Result.success();
	}

	@PostMapping("/{taskId}/cancel")
	public Result<Void> cancelTask(@PathVariable("taskId") Long taskId, @RequestBody(required = false) CancelTaskRequest request, @RequestHeader("X-User-Id") Long userId) {
		taskCommandAppService.cancelTask(taskId, userId, request == null ? null : request.getReason());
		return Result.success();
	}
}

