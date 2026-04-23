package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskCommandAppService;
import com.example.cae.task.interfaces.request.CancelTaskRequest;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.request.DiscardTaskRequest;
import com.example.cae.task.interfaces.request.UpdateTaskRequest;
import com.example.cae.task.interfaces.response.TaskActionResponse;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import com.example.cae.task.interfaces.response.TaskFileUploadResponse;
import com.example.cae.task.interfaces.response.TaskSubmitResponse;
import com.example.cae.task.interfaces.response.TaskUpdateResponse;
import com.example.cae.task.interfaces.response.TaskValidateResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
	private final TaskCommandAppService taskCommandAppService;

	public TaskController(TaskCommandAppService taskCommandAppService) {
		this.taskCommandAppService = taskCommandAppService;
	}

	@PostMapping
	public Result<TaskCreateResponse> createTask(@Valid @RequestBody CreateTaskRequest request, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId) {
		return Result.success(taskCommandAppService.createTask(request, userId));
	}

	@PutMapping("/{taskId}")
	public Result<TaskUpdateResponse> updateTask(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
											 @Valid @RequestBody UpdateTaskRequest request,
											 @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId) {
		return Result.success(taskCommandAppService.updateTask(taskId, request, userId));
	}

	@PostMapping("/{taskId}/files")
	public Result<TaskFileUploadResponse> uploadTaskFile(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
											 @RequestPart("file") MultipartFile file,
											 @RequestParam(value = "fileKey", required = false) String fileKey,
											 @RequestParam(value = "fileRole", required = false) String fileRole,
											 @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId) {
		return Result.success(taskCommandAppService.uploadTaskFile(taskId, file, fileKey, fileRole, userId));
	}

	@PostMapping("/{taskId}/validate")
	public Result<TaskValidateResponse> validateTask(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId) {
		return Result.success(taskCommandAppService.validateTask(taskId, userId));
	}

	@PostMapping("/{taskId}/submit")
	public Result<TaskSubmitResponse> submitTask(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId) {
		return Result.success(taskCommandAppService.submitTask(taskId, userId));
	}

	@PostMapping("/{taskId}/discard")
	public Result<Void> discardTask(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId, @Valid @RequestBody(required = false) DiscardTaskRequest request, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId) {
		taskCommandAppService.discardTask(taskId, userId, request == null ? null : request.getReason());
		return Result.success();
	}

	@PostMapping("/{taskId}/cancel")
	public Result<TaskActionResponse> cancelTask(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId, @Valid @RequestBody(required = false) CancelTaskRequest request, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId) {
		return Result.success(taskCommandAppService.cancelTask(taskId, userId, request == null ? null : request.getReason()));
	}
}

