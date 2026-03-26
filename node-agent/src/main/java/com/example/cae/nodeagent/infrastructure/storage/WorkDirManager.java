package com.example.cae.nodeagent.infrastructure.storage;

import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class WorkDirManager {
	private final NodeAgentConfig nodeAgentConfig;

	public WorkDirManager(NodeAgentConfig nodeAgentConfig) {
		this.nodeAgentConfig = nodeAgentConfig;
	}

	public void prepareTaskDirs(ExecutionContext context) {
		String root = nodeAgentConfig.getWorkRoot() + File.separator + context.getTaskId();
		context.setWorkDir(root);
		context.setInputDir(root + File.separator + "input");
		context.setOutputDir(root + File.separator + "output");
		context.setLogDir(root + File.separator + "log");

		new File(context.getInputDir()).mkdirs();
		new File(context.getOutputDir()).mkdirs();
		new File(context.getLogDir()).mkdirs();
	}

	public void cleanupTaskDirs(Long taskId) {
		// reserved for future cleanup policy
	}
}

