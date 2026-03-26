package com.example.cae.nodeagent.application.assembler;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.service.ExecutionContextBuildService;
import com.example.cae.nodeagent.interfaces.request.DispatchTaskRequest;
import org.springframework.stereotype.Component;

@Component
public class ExecutionContextAssembler {
	private final ExecutionContextBuildService executionContextBuildService;

	public ExecutionContextAssembler(ExecutionContextBuildService executionContextBuildService) {
		this.executionContextBuildService = executionContextBuildService;
	}

	public ExecutionContext fromDispatchRequest(DispatchTaskRequest request) {
		return executionContextBuildService.build(request);
	}
}