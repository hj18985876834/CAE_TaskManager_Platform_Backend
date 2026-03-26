package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskResultAppService;
import com.example.cae.task.interfaces.response.TaskResultFileResponse;
import com.example.cae.task.interfaces.response.TaskResultSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskResultController {
	private final TaskResultAppService taskResultAppService;

	public TaskResultController(TaskResultAppService taskResultAppService) {
		this.taskResultAppService = taskResultAppService;
	}

	@GetMapping("/{taskId}/result-summary")
	public Result<TaskResultSummaryResponse> getResultSummary(@PathVariable Long taskId, @RequestHeader("X-User-Id") Long userId, @RequestHeader(value = "X-Role-Code", required = false) String roleCode) {
		return Result.success(taskResultAppService.getResultSummary(taskId, userId, roleCode));
	}

	@GetMapping("/{taskId}/result-files")
	public Result<List<TaskResultFileResponse>> getResultFiles(@PathVariable Long taskId, @RequestHeader("X-User-Id") Long userId, @RequestHeader(value = "X-Role-Code", required = false) String roleCode) {
		return Result.success(taskResultAppService.getResultFiles(taskId, userId, roleCode));
	}
}

