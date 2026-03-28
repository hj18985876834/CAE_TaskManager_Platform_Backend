package com.example.cae.scheduler.interfaces.internal;

import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.service.NodeAppService;
import com.example.cae.scheduler.application.service.ScheduleAppService;
import com.example.cae.scheduler.interfaces.request.InternalScheduleRecordRequest;
import com.example.cae.scheduler.interfaces.request.UpdateRunningCountRequest;
import com.example.cae.scheduler.interfaces.response.AvailableNodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal")
public class InternalSchedulerController {
	private final NodeAppService nodeAppService;
	private final ScheduleAppService scheduleAppService;

	public InternalSchedulerController(NodeAppService nodeAppService, ScheduleAppService scheduleAppService) {
		this.nodeAppService = nodeAppService;
		this.scheduleAppService = scheduleAppService;
	}

	@GetMapping("/nodes/available")
	public Result<List<AvailableNodeResponse>> listAvailableNodes(@RequestParam Long solverId) {
		return Result.success(nodeAppService.listAvailableNodes(solverId));
	}

	@PostMapping("/schedules")
	public Result<Void> createScheduleRecord(@RequestBody InternalScheduleRecordRequest request) {
		scheduleAppService.recordSchedule(request);
		return Result.success();
	}

	@PostMapping("/nodes/{nodeId}/running-count")
	public Result<Void> updateRunningCount(@PathVariable Long nodeId, @RequestBody UpdateRunningCountRequest request) {
		nodeAppService.updateRunningCount(nodeId, request == null ? null : request.getDelta());
		return Result.success();
	}

	@GetMapping("/nodes/{nodeId}/token/verify")
	public Result<Boolean> verifyNodeToken(@PathVariable Long nodeId, @RequestParam String nodeToken) {
		return Result.success(nodeAppService.validateNodeToken(nodeId, nodeToken));
	}
}
