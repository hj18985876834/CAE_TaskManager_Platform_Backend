package com.example.cae.task.interfaces.internal;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.dto.TaskDispatchAckDTO;
import com.example.cae.common.dto.TaskScheduleClaimDTO;
import com.example.cae.common.dto.TaskStatusAckDTO;
import com.example.cae.common.response.Result;
import com.example.cae.task.application.manager.TaskDispatchManager;
import com.example.cae.task.interfaces.request.InternalTaskFailRequest;
import com.example.cae.task.interfaces.request.NodeOfflineTasksRequest;
import com.example.cae.task.interfaces.request.TaskNodeMarkRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/tasks")
public class InternalTaskDispatchController {
	private final TaskDispatchManager taskDispatchManager;

	public InternalTaskDispatchController(TaskDispatchManager taskDispatchManager) {
		this.taskDispatchManager = taskDispatchManager;
	}

	@GetMapping("/queued")
	public Result<List<TaskDTO>> listQueuedTasks(@RequestParam(value = "limit", required = false) Integer limit) {
		return Result.success(taskDispatchManager.listQueuedTasks(limit));
	}

	@PostMapping("/{taskId}/mark-scheduled")
	public Result<TaskScheduleClaimDTO> markScheduled(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
								 @Valid @RequestBody TaskNodeMarkRequest request) {
		return Result.success(taskDispatchManager.markScheduled(taskId, request.getNodeId()));
	}

	@PostMapping("/{taskId}/mark-dispatched")
	public Result<TaskDispatchAckDTO> markDispatched(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
								  @Valid @RequestBody TaskNodeMarkRequest request) {
		return Result.success(taskDispatchManager.markDispatched(taskId, request.getNodeId()));
	}

	@PostMapping("/{taskId}/dispatch-failed")
	public Result<TaskStatusAckDTO> markFailed(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
										  @Valid @RequestBody InternalTaskFailRequest request) {
		return Result.success(taskDispatchManager.markFailed(taskId, request.getNodeId(), request.getFailType(), request.getReason(), request.getRecoverable()));
	}

	@PostMapping("/node-offline/fail")
	public Result<Integer> markNodeOfflineTasksFailed(@Valid @RequestBody NodeOfflineTasksRequest request) {
		return Result.success(taskDispatchManager.markNodeOfflineTasksFailed(request.getNodeId(), request.getReason()));
	}
}
