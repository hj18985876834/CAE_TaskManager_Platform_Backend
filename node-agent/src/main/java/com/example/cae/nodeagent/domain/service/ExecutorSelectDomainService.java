package com.example.cae.nodeagent.domain.service;

import com.example.cae.common.exception.BizException;
import com.example.cae.nodeagent.domain.executor.SolverExecutor;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExecutorSelectDomainService {
	private final List<SolverExecutor> executors;

	public ExecutorSelectDomainService(List<SolverExecutor> executors) {
		this.executors = executors;
	}

	public SolverExecutor selectExecutor(ExecutionContext context) {
		return executors.stream()
				.filter(executor -> executor.supports(context))
				.findFirst()
				.orElseThrow(() -> new BizException("no available executor for solver"));
	}
}

