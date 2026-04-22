package com.example.cae.task.interfaces.internal;

import com.example.cae.common.dto.TaskBasicDTO;
import com.example.cae.common.response.Result;
import com.example.cae.task.application.service.TaskQueryAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/tasks")
public class InternalTaskQueryController {
	private final TaskQueryAppService taskQueryAppService;

	public InternalTaskQueryController(TaskQueryAppService taskQueryAppService) {
		this.taskQueryAppService = taskQueryAppService;
	}

	@GetMapping("/basics")
	public Result<List<TaskBasicDTO>> listTaskBasics(@RequestParam(value = "taskIds", required = false) List<Long> taskIds) {
		return Result.success(taskQueryAppService.listTaskBasics(taskIds));
	}
}
