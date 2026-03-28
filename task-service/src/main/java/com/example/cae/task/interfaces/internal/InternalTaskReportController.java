package com.example.cae.task.interfaces.internal;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.response.Result;
import com.example.cae.task.application.manager.TaskLifecycleManager;
import com.example.cae.task.application.manager.TaskResultManager;
import com.example.cae.task.application.service.NodeAgentAuthService;
import com.example.cae.task.interfaces.request.LogReportRequest;
import com.example.cae.task.interfaces.request.MarkFailedRequest;
import com.example.cae.task.interfaces.request.MarkFinishedRequest;
import com.example.cae.task.interfaces.request.ResultFileReportRequest;
import com.example.cae.task.interfaces.request.ResultSummaryReportRequest;
import com.example.cae.task.interfaces.request.StatusReportRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
	public Result<Void> reportStatus(@PathVariable("taskId") Long taskId,
								 @RequestBody StatusReportRequest request,
								 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request == null ? null : request.getNodeId(), nodeToken);
		taskLifecycleManager.reportStatus(taskId, request);
		return Result.success();
	}

	@PostMapping("/{taskId}/log-report")
	public Result<Void> reportLog(@PathVariable("taskId") Long taskId,
							  @RequestBody LogReportRequest request,
							  @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request == null ? null : request.getNodeId(), nodeToken);
		taskResultManager.appendLog(taskId, request.getSeqNo(), request.getLogContent());
		return Result.success();
	}

	@PostMapping("/{taskId}/result-summary-report")
	public Result<Void> reportResultSummary(@PathVariable("taskId") Long taskId,
									@RequestBody ResultSummaryReportRequest request,
									@RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request == null ? null : request.getNodeId(), nodeToken);
		taskResultManager.saveResultSummary(taskId, request);
		return Result.success();
	}

	@PostMapping("/{taskId}/result-file-report")
	public Result<Void> reportResultFile(@PathVariable("taskId") Long taskId,
								 @RequestBody ResultFileReportRequest request,
								 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request == null ? null : request.getNodeId(), nodeToken);
		taskResultManager.saveResultFile(taskId, request);
		return Result.success();
	}

	@PostMapping("/{taskId}/mark-finished")
	public Result<Void> markFinished(@PathVariable("taskId") Long taskId,
								 @RequestBody MarkFinishedRequest request,
								 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request == null ? null : request.getNodeId(), nodeToken);
		taskResultManager.finishTask(taskId, request == null ? null : request.getFinalStatus());
		return Result.success();
	}

	@PostMapping("/{taskId}/mark-failed")
	public Result<Void> markFailed(@PathVariable("taskId") Long taskId,
							   @RequestBody MarkFailedRequest request,
							   @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = false) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request == null ? null : request.getNodeId(), nodeToken);
		taskResultManager.failTask(taskId, request == null ? null : request.getFailType(), request == null ? null : request.getFailMessage());
		return Result.success();
	}
}

