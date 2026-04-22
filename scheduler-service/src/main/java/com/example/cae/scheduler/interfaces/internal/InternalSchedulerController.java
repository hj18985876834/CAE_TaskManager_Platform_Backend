package com.example.cae.scheduler.interfaces.internal;

import com.example.cae.common.dto.TaskStatusAckDTO;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.service.NodeAppService;
import com.example.cae.scheduler.application.service.ScheduleAppService;
import com.example.cae.scheduler.interfaces.request.InternalTaskDispatchFailureRequest;
import com.example.cae.scheduler.interfaces.request.InternalScheduleRecordRequest;
import com.example.cae.scheduler.interfaces.request.NodeTaskCancelRequest;
import com.example.cae.scheduler.interfaces.response.AvailableNodeResponse;
import com.example.cae.scheduler.interfaces.response.ScheduleRecordResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
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
	public Result<List<AvailableNodeResponse>> listAvailableNodes(@RequestParam @Positive(message = "solverId必须大于0") Long solverId) {
		return Result.success(nodeAppService.listAvailableNodes(solverId));
	}

	@PostMapping("/schedules")
	public Result<Void> createScheduleRecord(@Valid @RequestBody InternalScheduleRecordRequest request) {
		scheduleAppService.recordSchedule(request);
		return Result.success();
	}

	@PostMapping("/tasks/{taskId}/dispatch-failed")
	public Result<TaskStatusAckDTO> dispatchFailed(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
												   @Valid @RequestBody InternalTaskDispatchFailureRequest request) {
		return Result.success(scheduleAppService.handleDispatchFailure(
				taskId,
				request.getNodeId(),
				request.getFailType(),
				request.getReason(),
				Boolean.TRUE.equals(request.getRecoverable())
		));
	}

	@GetMapping("/tasks/{taskId}/schedules")
	public Result<List<ScheduleRecordResponse>> listTaskSchedules(@PathVariable("taskId") @Positive(message = "taskId蹇呴』澶т簬0") Long taskId) {
		return Result.success(scheduleAppService.listByTaskId(taskId));
	}

	@PostMapping("/nodes/{nodeId}/cancel-task")
	public Result<Void> cancelTask(@PathVariable @Positive(message = "nodeId必须大于0") Long nodeId,
								   @Valid @RequestBody NodeTaskCancelRequest request) {
		scheduleAppService.cancelTaskOnNode(nodeId, request.getTaskId(), request.getReason());
		return Result.success();
	}

	@GetMapping("/nodes/{nodeId}/token/verify")
	public Result<Boolean> verifyNodeToken(@PathVariable @Positive(message = "nodeId必须大于0") Long nodeId,
										   @RequestParam @NotBlank(message = "nodeToken不能为空") String nodeToken) {
		return Result.success(nodeAppService.validateNodeToken(nodeId, nodeToken));
	}
}
