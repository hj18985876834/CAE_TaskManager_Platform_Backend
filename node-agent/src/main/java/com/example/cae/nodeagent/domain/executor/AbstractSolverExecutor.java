package com.example.cae.nodeagent.domain.executor;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;

import java.io.File;

public abstract class AbstractSolverExecutor implements SolverExecutor {
	@Override
	public ExecutionResult execute(ExecutionContext context) {
		prepare(context);
		return doExecute(context);
	}

	protected void prepare(ExecutionContext context) {
		// default no-op
	}

	protected File resolveProcessWorkDir(ExecutionContext context) {
		String taskDir = context == null ? null : context.getTaskDir();
		if (taskDir != null && !taskDir.isBlank()) {
			return new File(taskDir);
		}
		return new File(context.getWorkDir());
	}

	protected abstract ExecutionResult doExecute(ExecutionContext context);
}
