package com.example.cae.nodeagent.domain.executor;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;

public interface SolverExecutor {
	boolean supports(ExecutionContext context);

	default void preflight(ExecutionContext context) {
	}

	ExecutionResult execute(ExecutionContext context);
}
