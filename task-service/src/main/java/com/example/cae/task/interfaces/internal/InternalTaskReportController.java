package com.example.cae.task.interfaces.internal;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.dto.TaskStatusAckDTO;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
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
	public Result<TaskStatusAckDTO> reportStatus(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
								 @Valid @RequestBody StatusReportRequest request,
								 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request.getNodeId(), nodeToken);
		return Result.success(taskLifecycleManager.reportStatus(taskId, request));
	}

	@PostMapping("/{taskId}/log-report")
	public Result<Void> reportLog(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
							  @Valid @RequestBody LogReportRequest request,
							  @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request.getNodeId(), nodeToken);
		taskResultManager.appendLog(taskId, request.getSeqNo(), request.getLogContent());
		return Result.success();
	}

	@PostMapping("/{taskId}/result-summary-report")
	public Result<Void> reportResultSummary(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
									@Valid @RequestBody ResultSummaryReportRequest request,
									@RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request.getNodeId(), nodeToken);
		taskResultManager.saveResultSummary(taskId, request);
		return Result.success();
	}

	@PostMapping("/{taskId}/result-file-report")
	public Result<Void> reportResultFile(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
								 @Valid @RequestBody ResultFileReportRequest request,
								 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request.getNodeId(), nodeToken);
		taskResultManager.saveResultFile(taskId, request);
		return Result.success();
	}

	@PostMapping("/{taskId}/mark-finished")
	public Result<TaskStatusAckDTO> markFinished(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
								 @Valid @RequestBody MarkFinishedRequest request,
								 @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request.getNodeId(), nodeToken);
		return Result.success(taskResultManager.finishTask(taskId, request.getFinalStatus()));
	}

	@PostMapping("/{taskId}/mark-failed")
	public Result<TaskStatusAckDTO> markFailed(@PathVariable("taskId") @Positive(message = "taskId必须大于0") Long taskId,
							   @Valid @RequestBody MarkFailedRequest request,
							   @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		nodeAgentAuthService.validateTaskNodeToken(taskId, request.getNodeId(), nodeToken);
		return Result.success(taskResultManager.failTask(taskId, request.getFailType(), request.getFailMessage()));
	}
}
