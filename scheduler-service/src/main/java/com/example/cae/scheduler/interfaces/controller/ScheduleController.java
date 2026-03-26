package com.example.cae.scheduler.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.facade.ScheduleFacade;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;
import com.example.cae.scheduler.interfaces.response.ScheduleRecordResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler/records")
public class ScheduleController {
	private final ScheduleFacade scheduleFacade;

	public ScheduleController(ScheduleFacade scheduleFacade) {
		this.scheduleFacade = scheduleFacade;
	}

	@GetMapping
	public Result<PageResult<ScheduleRecordResponse>> pageRecords(SchedulePageQueryRequest request) {
		return Result.success(scheduleFacade.pageRecords(request));
	}
}

