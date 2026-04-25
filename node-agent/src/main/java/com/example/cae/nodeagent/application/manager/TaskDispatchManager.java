package com.example.cae.nodeagent.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.nodeagent.application.assembler.ExecutionContextAssembler;
import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.interfaces.request.CancelTaskRequest;
import com.example.cae.nodeagent.interfaces.request.DispatchTaskRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Service
public class TaskDispatchManager {
	private final ExecutionContextAssembler executionContextAssembler;
	private final TaskExecuteManager taskExecuteManager;
	private final TaskReportManager taskReportManager;
	private final TaskRuntimeRegistry taskRuntimeRegistry;
	private final NodeAgentConfig nodeAgentConfig;
	private final Executor taskExecutor;

	public TaskDispatchManager(ExecutionContextAssembler executionContextAssembler,
						   TaskExecuteManager taskExecuteManager,
						   TaskReportManager taskReportManager,
						   TaskRuntimeRegistry taskRuntimeRegistry,
						   NodeAgentConfig nodeAgentConfig,
						   @Qualifier("taskExecutor") Executor taskExecutor) {
		this.executionContextAssembler = executionContextAssembler;
		this.taskExecuteManager = taskExecuteManager;
		this.taskReportManager = taskReportManager;
		this.taskRuntimeRegistry = taskRuntimeRegistry;
		this.nodeAgentConfig = nodeAgentConfig;
		this.taskExecutor = taskExecutor;
	}

	public void acceptTask(DispatchTaskRequest request) {
		if (request == null || request.getTaskId() == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "taskId is required");
		}
		if (taskRuntimeRegistry.isRunning(request.getTaskId())) {
			return;
		}
		if (taskRuntimeRegistry.activeCount() >= maxConcurrency()) {
			throw new BizException(ErrorCodeConstants.CONFLICT, "node is busy");
		}
		if (!taskRuntimeRegistry.register(request.getTaskId())) {
			return;
		}
		ExecutionContext context = executionContextAssembler.fromDispatchRequest(request);
		try {
			taskReportManager.onTaskAccepted(request.getTaskId());
			taskExecutor.execute(() -> taskExecuteManager.execute(context));
		} catch (RuntimeException ex) {
			taskRuntimeRegistry.finish(request.getTaskId());
			throw ex;
		}
	}

	public boolean cancelTask(CancelTaskRequest request) {
		if (request == null || request.getTaskId() == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "taskId is required");
		}
		return taskRuntimeRegistry.cancel(request.getTaskId(), request.getReason());
	}

	private int maxConcurrency() {
		return nodeAgentConfig.getMaxConcurrency() == null || nodeAgentConfig.getMaxConcurrency() <= 0
				? 1
				: nodeAgentConfig.getMaxConcurrency();
	}
}
