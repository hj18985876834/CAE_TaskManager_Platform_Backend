package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskLogAppService;
import com.example.cae.task.interfaces.response.TaskLogResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskLogController {
	private final TaskLogAppService taskLogAppService;

	public TaskLogController(TaskLogAppService taskLogAppService) {
		this.taskLogAppService = taskLogAppService;
	}

	@GetMapping("/{taskId}/logs")
	public Result<List<TaskLogResponse>> getLogs(
			@PathVariable Long taskId,
			@RequestParam(defaultValue = "0") Integer fromSeq,
			@RequestParam(defaultValue = "100") Integer pageSize,
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader(value = "X-Role-Code", required = false) String roleCode
	) {
		return Result.success(taskLogAppService.getLogs(taskId, fromSeq, pageSize, userId, roleCode));
	}
}

