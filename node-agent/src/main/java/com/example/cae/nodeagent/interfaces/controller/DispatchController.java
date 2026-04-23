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
	public Result<DispatchTaskResponse> dispatch(@RequestBody DispatchTaskRequest request,
											 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		ensureValidNodeToken(nodeToken);
		taskDispatchManager.acceptTask(request);
		DispatchTaskResponse response = new DispatchTaskResponse();
		response.setAccepted(true);
		response.setMessage("task accepted");
		return Result.success(response);
	}

	@PostMapping("/cancel-task")
	public Result<CancelTaskResponse> cancel(@RequestBody CancelTaskRequest request,
										 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		ensureValidNodeToken(nodeToken);
		throw new BizException(ErrorCodeConstants.TASK_STATUS_UNSUPPORTED,
				"runtime cancel is not supported in first-version status contract");
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
