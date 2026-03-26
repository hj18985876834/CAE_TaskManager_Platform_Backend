package com.example.cae.nodeagent.domain.executor;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;

public abstract class AbstractSolverExecutor implements SolverExecutor {
	@Override
	public ExecutionResult execute(ExecutionContext context) {
		prepare(context);
		return doExecute(context);
	}

	protected void prepare(ExecutionContext context) {
		// default no-op
	}

	protected abstract ExecutionResult doExecute(ExecutionContext context);
}

