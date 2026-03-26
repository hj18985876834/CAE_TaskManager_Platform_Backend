package com.example.cae.nodeagent.domain.service;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.interfaces.request.DispatchTaskRequest;
import org.springframework.stereotype.Service;

@Service
public class ExecutionContextBuildService {
	public ExecutionContext build(DispatchTaskRequest request) {
		ExecutionContext context = new ExecutionContext();
		context.setTaskId(request.getTaskId());
		context.setTaskNo(request.getTaskNo());
		context.setSolverId(request.getSolverId());
		context.setSolverCode(request.getSolverCode());
		context.setProfileId(request.getProfileId());
		context.setTaskType(request.getTaskType());
		context.setCommandTemplate(request.getCommandTemplate());
		context.setParserName(request.getParserName());
		context.setTimeoutSeconds(request.getTimeoutSeconds());
		context.setInputFiles(request.getInputFiles());
		context.setParams(request.getParams());
		return context;
	}
}

