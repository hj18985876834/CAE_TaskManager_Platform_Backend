package com.example.cae.nodeagent.application.manager;

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
	private final Executor taskExecutor;
	private final NodeAgentConfig nodeAgentConfig;

	public TaskDispatchManager(ExecutionContextAssembler executionContextAssembler,
						   TaskExecuteManager taskExecuteManager,
						   TaskReportManager taskReportManager,
						   @Qualifier("taskExecutor") Executor taskExecutor,
						   NodeAgentConfig nodeAgentConfig) {
		this.executionContextAssembler = executionContextAssembler;
		this.taskExecuteManager = taskExecuteManager;
		this.taskReportManager = taskReportManager;
		this.taskExecutor = taskExecutor;
		this.nodeAgentConfig = nodeAgentConfig;
	}

	public void acceptTask(DispatchTaskRequest request) {
		ExecutionContext context = executionContextAssembler.fromDispatchRequest(request);
		taskReportManager.reportDispatched(context.getTaskId(), nodeAgentConfig.getNodeId());
		taskExecutor.execute(() -> taskExecuteManager.execute(context));
	}

	public void cancelTask(CancelTaskRequest request) {
		// reserved for future: find process by taskId and interrupt it
	}
}

