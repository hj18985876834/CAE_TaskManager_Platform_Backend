package com.example.cae.task.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskQueryAppService;
import com.example.cae.task.interfaces.response.TaskDashboardSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
	private final TaskQueryAppService taskQueryAppService;

	public AdminDashboardController(TaskQueryAppService taskQueryAppService) {
		this.taskQueryAppService = taskQueryAppService;
	}

	@GetMapping("/summary")
	public Result<TaskDashboardSummaryResponse> getDashboardSummary() {
		return Result.success(taskQueryAppService.getDashboardSummary());
	}
}
