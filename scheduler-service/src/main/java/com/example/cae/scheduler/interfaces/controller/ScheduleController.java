package com.example.cae.scheduler.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.facade.ScheduleFacade;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;
import com.example.cae.scheduler.interfaces.response.ScheduleRecordResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
public class ScheduleController {
	private final ScheduleFacade scheduleFacade;

	public ScheduleController(ScheduleFacade scheduleFacade) {
		this.scheduleFacade = scheduleFacade;
	}

	@GetMapping("/schedules")
	public Result<PageResult<ScheduleRecordResponse>> pageRecords(@Valid SchedulePageQueryRequest request) {
		return Result.success(scheduleFacade.pageRecords(request));
	}

	@GetMapping("/tasks/{taskId}/schedules")
	public Result<List<ScheduleRecordResponse>> listByTaskId(@PathVariable @Positive(message = "taskId必须大于0") Long taskId) {
		return Result.success(scheduleFacade.listByTaskId(taskId));
	}
}
