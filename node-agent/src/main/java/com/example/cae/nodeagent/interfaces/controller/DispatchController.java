package com.example.cae.nodeagent.interfaces.controller;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.Result;
import com.example.cae.nodeagent.application.manager.TaskDispatchManager;
import com.example.cae.nodeagent.interfaces.request.CancelTaskRequest;
import com.example.cae.nodeagent.interfaces.request.DispatchTaskRequest;
import com.example.cae.nodeagent.interfaces.response.CancelTaskResponse;
import com.example.cae.nodeagent.interfaces.response.DispatchTaskResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class DispatchController {
	private final TaskDispatchManager taskDispatchManager;

	public DispatchController(TaskDispatchManager taskDispatchManager) {
		this.taskDispatchManager = taskDispatchManager;
	}

	@PostMapping("/dispatch-task")
	public Result<DispatchTaskResponse> dispatch(@RequestBody DispatchTaskRequest request) {
		taskDispatchManager.acceptTask(request);
		DispatchTaskResponse response = new DispatchTaskResponse();
		response.setAccepted(true);
		response.setMessage("task accepted");
		return Result.success(response);
	}

	@PostMapping("/cancel-task")
	public Result<CancelTaskResponse> cancel(@RequestBody CancelTaskRequest request) {
		throw new BizException(ErrorCodeConstants.TASK_STATUS_UNSUPPORTED,
				"runtime cancel is not supported in first-version status contract");
	}
}
