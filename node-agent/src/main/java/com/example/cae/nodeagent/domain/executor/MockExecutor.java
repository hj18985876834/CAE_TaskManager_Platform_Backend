package com.example.cae.nodeagent.domain.executor;

import com.example.cae.nodeagent.application.manager.TaskReportManager;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class MockExecutor extends AbstractSolverExecutor {
	private final TaskReportManager taskReportManager;

	public MockExecutor(TaskReportManager taskReportManager) {
		this.taskReportManager = taskReportManager;
	}

	@Override
	public boolean supports(ExecutionContext context) {
		return context != null
				&& ("MOCK".equalsIgnoreCase(context.getSolverCode())
				|| context.getSolverId() == null
				|| context.getSolverId() == 0L);
	}

	@Override
	protected ExecutionResult doExecute(ExecutionContext context) {
		long start = System.currentTimeMillis();
		try {
			for (int i = 1; i <= 5; i++) {
				Thread.sleep(300);
				taskReportManager.pushLog(context.getTaskId(), i, "mock running step " + i);
			}
			int duration = (int) ((System.currentTimeMillis() - start) / 1000);
			return ExecutionResult.success(duration, "mock solver execute success", Map.of("iterations", 5), Collections.emptyList());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return ExecutionResult.fail((int) ((System.currentTimeMillis() - start) / 1000), "mock execute interrupted");
		}
	}
}

