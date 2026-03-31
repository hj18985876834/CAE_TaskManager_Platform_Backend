package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskResultAppService;
import com.example.cae.task.domain.model.TaskResultFile;
import com.example.cae.task.interfaces.response.TaskResultFileResponse;
import com.example.cae.task.interfaces.response.TaskResultSummaryResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/tasks")
public class TaskResultController {
	private final TaskResultAppService taskResultAppService;

	public TaskResultController(TaskResultAppService taskResultAppService) {
		this.taskResultAppService = taskResultAppService;
	}

	@GetMapping("/{taskId}/result-summary")
	public Result<TaskResultSummaryResponse> getResultSummary(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId, @RequestHeader(value = "X-Role-Code", required = false) String roleCode) {
		return Result.success(taskResultAppService.getResultSummary(taskId, userId, roleCode));
	}

	@GetMapping("/{taskId}/result-files")
	public Result<List<TaskResultFileResponse>> getResultFiles(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId, @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId, @RequestHeader(value = "X-Role-Code", required = false) String roleCode) {
		return Result.success(taskResultAppService.getResultFiles(taskId, userId, roleCode));
	}

	@GetMapping("/result-files/{fileId}/download")
	public ResponseEntity<InputStreamResource> downloadResultFile(@PathVariable("fileId") @Positive(message = "fileId必须大于0") Long fileId,
															 @RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId,
															 @RequestHeader(value = "X-Role-Code", required = false) String roleCode) {
		TaskResultFile file = taskResultAppService.getResultFile(fileId, userId, roleCode);
		InputStream inputStream = taskResultAppService.openResultFile(file);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
				.contentLength(file.getFileSize() == null ? 0L : file.getFileSize())
				.body(new InputStreamResource(inputStream));
	}
}
