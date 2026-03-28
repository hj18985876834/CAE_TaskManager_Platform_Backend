package com.example.cae.task.interfaces.internal;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.response.Result;
import com.example.cae.task.application.manager.TaskLifecycleManager;
import com.example.cae.task.application.manager.TaskResultManager;
import com.example.cae.task.application.service.NodeAgentAuthService;
import com.example.cae.task.interfaces.request.LogReportRequest;
import com.example.cae.task.interfaces.request.ResultFileReportRequest;
import com.example.cae.task.interfaces.request.ResultSummaryReportRequest;
import com.example.cae.task.interfaces.request.StatusReportRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/tasks")
public class InternalTaskReportController {
	private final TaskResultManager taskResultManager;
	private final TaskLifecycleManager taskLifecycleManager;
	private final NodeAgentAuthService nodeAgentAuthService;

	public InternalTaskReportController(TaskResultManager taskResultManager,
									TaskLifecycleManager taskLifecycleManager,
									NodeAgentAuthService nodeAgentAuthService) {
		this.taskResultManager = taskResultManager;
		this.taskLifecycleManager = taskLifecycleManager;
		this.nodeAgentAuthService = nodeAgentAuthService;
	}

	@PostMapping("/{taskId}/status-report")
	public Result<Void> reportStatus(@PathVariable Long taskId,
								 @RequestBody StatusReportRequest request,
								 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, nodeToken);
		taskLifecycleManager.reportStatus(taskId, request);
		return Result.success();
	}

	@PostMapping("/{taskId}/log-report")
	public Result<Void> reportLog(@PathVariable Long taskId,
							  @RequestBody LogReportRequest request,
							  @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, nodeToken);
		taskResultManager.appendLog(taskId, request.getSeqNo(), request.getContent());
		return Result.success();
	}

	@PostMapping("/{taskId}/result-summary-report")
	public Result<Void> reportResultSummary(@PathVariable Long taskId,
									@RequestBody ResultSummaryReportRequest request,
									@RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, nodeToken);
		taskResultManager.saveResultSummary(taskId, request);
		return Result.success();
	}

	@PostMapping("/{taskId}/result-file-report")
	public Result<Void> reportResultFile(@PathVariable Long taskId,
								 @RequestBody ResultFileReportRequest request,
								 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, nodeToken);
		taskResultManager.saveResultFile(taskId, request);
		return Result.success();
	}

	@PostMapping("/{taskId}/mark-finished")
	public Result<Void> markFinished(@PathVariable Long taskId,
								 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, nodeToken);
		taskResultManager.finishTask(taskId);
		return Result.success();
	}

	@PostMapping("/{taskId}/mark-failed")
	public Result<Void> markFailed(@PathVariable Long taskId,
							   @RequestParam String failType,
							   @RequestParam String failMessage,
							   @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, nodeToken);
		taskResultManager.failTask(taskId, failType, failMessage);
		return Result.success();
	}
}

