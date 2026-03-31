package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskLogAppService;
import com.example.cae.task.interfaces.response.TaskLogPageResponse;
import com.example.cae.task.interfaces.response.TaskLogResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/tasks")
public class TaskLogController {
	private final TaskLogAppService taskLogAppService;

	public TaskLogController(TaskLogAppService taskLogAppService) {
		this.taskLogAppService = taskLogAppService;
	}

	@GetMapping("/{taskId}/logs")
	public Result<TaskLogPageResponse> getLogs(
			@PathVariable("taskId") Long taskId,
			@RequestParam(value = "fromSeq", defaultValue = "0") Integer fromSeq,
			@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize,
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader(value = "X-Role-Code", required = false) String roleCode
	) {
		return Result.success(taskLogAppService.getLogs(taskId, fromSeq, pageSize, userId, roleCode));
	}

	@GetMapping("/{taskId}/logs/download")
	public ResponseEntity<ByteArrayResource> downloadLogs(
			@PathVariable("taskId") Long taskId,
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader(value = "X-Role-Code", required = false) String roleCode
	) {
		byte[] content = taskLogAppService.getFullLogContent(taskId, userId, roleCode).getBytes(StandardCharsets.UTF_8);
		return ResponseEntity.ok()
				.contentType(MediaType.TEXT_PLAIN)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"task-" + taskId + ".log\"")
				.contentLength(content.length)
				.body(new ByteArrayResource(content));
	}
}
