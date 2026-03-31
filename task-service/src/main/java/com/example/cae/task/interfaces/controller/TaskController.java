package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskCommandAppService;
import com.example.cae.task.interfaces.request.CancelTaskRequest;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import com.example.cae.task.interfaces.response.TaskFileResponse;
import com.example.cae.task.interfaces.response.TaskSubmitResponse;
import com.example.cae.task.interfaces.response.TaskValidateResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	public Result<TaskFileResponse> uploadTaskFile(@PathVariable("taskId") Long taskId,
											 @RequestPart("file") MultipartFile file,
											 @RequestParam(value = "fileKey", required = false) String fileKey,
											 @RequestParam(value = "fileRole", required = false) String fileRole,
											 @RequestHeader("X-User-Id") Long userId) {
		return Result.success(taskCommandAppService.uploadTaskFile(taskId, file, fileKey, fileRole, userId));
	}

	@PostMapping("/{taskId}/validate")
	public Result<TaskValidateResponse> validateTask(@PathVariable("taskId") Long taskId, @RequestHeader("X-User-Id") Long userId) {
		return Result.success(taskCommandAppService.validateTask(taskId, userId));
	}

	@PostMapping("/{taskId}/submit")
	public Result<TaskSubmitResponse> submitTask(@PathVariable("taskId") Long taskId, @RequestHeader("X-User-Id") Long userId) {
		return Result.success(taskCommandAppService.submitTask(taskId, userId));
	}

	@PostMapping("/{taskId}/cancel")
	public Result<Void> cancelTask(@PathVariable("taskId") Long taskId, @RequestBody(required = false) CancelTaskRequest request, @RequestHeader("X-User-Id") Long userId) {
		taskCommandAppService.cancelTask(taskId, userId, request == null ? null : request.getReason());
		return Result.success();
	}
}
