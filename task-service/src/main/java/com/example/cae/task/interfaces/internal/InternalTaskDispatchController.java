package com.example.cae.task.interfaces.internal;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.response.Result;
import com.example.cae.task.application.manager.TaskDispatchManager;
import com.example.cae.task.interfaces.request.InternalTaskFailRequest;
import com.example.cae.task.interfaces.request.NodeOfflineTasksRequest;
import com.example.cae.task.interfaces.request.TaskNodeMarkRequest;
import jakarta.validation.Valid;
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
	public Result<Void> markScheduled(@PathVariable("taskId") Long taskId,
								 @RequestBody(required = false) TaskNodeMarkRequest request,
							 @RequestParam(value = "nodeId", required = false) Long nodeId) {
		Long effectiveNodeId = request != null && request.getNodeId() != null ? request.getNodeId() : nodeId;
		taskDispatchManager.markScheduled(taskId, effectiveNodeId);
		return Result.success();
	}

	@PostMapping("/{taskId}/mark-dispatched")
	public Result<Void> markDispatched(@PathVariable("taskId") Long taskId,
								  @RequestBody(required = false) TaskNodeMarkRequest request,
							  @RequestParam(value = "nodeId", required = false) Long nodeId) {
		Long effectiveNodeId = request != null && request.getNodeId() != null ? request.getNodeId() : nodeId;
		taskDispatchManager.markDispatched(taskId, effectiveNodeId);
		return Result.success();
	}

	@PostMapping("/{taskId}/mark-failed")
	public Result<Void> markFailed(@PathVariable("taskId") Long taskId, @Valid @RequestBody InternalTaskFailRequest request) {
		taskDispatchManager.markFailed(taskId, request.getFailType(), request.getReason());
		return Result.success();
	}

	@PostMapping("/node-offline/fail")
	public Result<Integer> markNodeOfflineTasksFailed(@Valid @RequestBody NodeOfflineTasksRequest request) {
		return Result.success(taskDispatchManager.markNodeOfflineTasksFailed(request.getNodeId(), request.getReason()));
	}
}
