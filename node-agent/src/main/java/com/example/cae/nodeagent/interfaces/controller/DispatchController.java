package com.example.cae.nodeagent.interfaces.controller;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.Result;
import com.example.cae.nodeagent.application.manager.TaskDispatchManager;
import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.interfaces.request.CancelTaskRequest;
import com.example.cae.nodeagent.interfaces.request.DispatchTaskRequest;
import com.example.cae.nodeagent.interfaces.response.CancelTaskResponse;
import com.example.cae.nodeagent.interfaces.response.DispatchTaskResponse;
import com.example.cae.nodeagent.interfaces.response.TaskRuntimeStatusResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class DispatchController {
	private final TaskDispatchManager taskDispatchManager;
	private final NodeAgentConfig nodeAgentConfig;

	public DispatchController(TaskDispatchManager taskDispatchManager, NodeAgentConfig nodeAgentConfig) {
		this.taskDispatchManager = taskDispatchManager;
		this.nodeAgentConfig = nodeAgentConfig;
	}

	@PostMapping("/dispatch-task")
	public Result<DispatchTaskResponse> dispatch(@Valid @RequestBody DispatchTaskRequest request,
											 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		ensureValidNodeToken(nodeToken);
		taskDispatchManager.acceptTask(request);
		DispatchTaskResponse response = new DispatchTaskResponse();
		response.setAccepted(true);
		response.setMessage("task accepted");
		return Result.success(response);
	}

	@PostMapping("/cancel-task")
	public Result<CancelTaskResponse> cancel(@Valid @RequestBody CancelTaskRequest request,
										 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		ensureValidNodeToken(nodeToken);
		throw new BizException(ErrorCodeConstants.TASK_STATUS_UNSUPPORTED,
				"runtime cancel is not supported in first-version status contract");
	}

	@GetMapping("/tasks/{taskId}/runtime")
	public Result<TaskRuntimeStatusResponse> runtimeStatus(@PathVariable Long taskId,
													  @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		ensureValidNodeToken(nodeToken);
		if (taskId == null || taskId <= 0) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "taskId is required");
		}
		TaskRuntimeStatusResponse response = new TaskRuntimeStatusResponse();
		response.setTaskId(taskId);
		response.setActive(taskDispatchManager.isTaskActive(taskId));
		response.setRunningReported(taskDispatchManager.isRunningReported(taskId));
		return Result.success(response);
	}

	private void ensureValidNodeToken(String nodeToken) {
		String expectedToken = nodeAgentConfig.getNodeToken();
		if (expectedToken == null || expectedToken.isBlank()) {
			throw new BizException(ErrorCodeConstants.FORBIDDEN, "node token is not configured");
		}
		if (nodeToken == null || nodeToken.isBlank()) {
			throw new BizException(ErrorCodeConstants.FORBIDDEN, "node token is required");
		}
		if (!expectedToken.equals(nodeToken)) {
			throw new BizException(ErrorCodeConstants.FORBIDDEN, "invalid node token");
		}
	}
}
