package com.example.cae.task.interfaces.internal;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.response.Result;
import com.example.cae.task.application.manager.TaskDispatchManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
	public Result<List<TaskDTO>> listQueuedTasks() {
		return Result.success(taskDispatchManager.listQueuedTasks());
	}

	@PostMapping("/{taskId}/mark-scheduled")
	public Result<Void> markScheduled(@PathVariable Long taskId, @RequestParam Long nodeId) {
		taskDispatchManager.markScheduled(taskId, nodeId);
		return Result.success();
	}

	@PostMapping("/{taskId}/mark-dispatched")
	public Result<Void> markDispatched(@PathVariable Long taskId, @RequestParam Long nodeId) {
		taskDispatchManager.markDispatched(taskId, nodeId);
		return Result.success();
	}
}

