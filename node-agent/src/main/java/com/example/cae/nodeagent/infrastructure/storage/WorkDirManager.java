package com.example.cae.nodeagent.infrastructure.storage;

import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.InputFileMeta;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class WorkDirManager {
	private final NodeAgentConfig nodeAgentConfig;
	private final PathMappingSupport pathMappingSupport;

	public WorkDirManager(NodeAgentConfig nodeAgentConfig, PathMappingSupport pathMappingSupport) {
		this.nodeAgentConfig = nodeAgentConfig;
		this.pathMappingSupport = pathMappingSupport;
	}

	public void prepareTaskDirs(ExecutionContext context) {
		Path root = resolveTaskRoot(context);
		Path inputDir = root.resolve("input");
		Path outputDir = root.resolve("output");
		Path logDir = root.resolve("log");

		context.setWorkDir(normalize(root));
		context.setTaskDir(normalize(root));
		context.setInputDir(normalize(inputDir));
		context.setOutputDir(normalize(outputDir));
		context.setLogDir(normalize(logDir));

		try {
			Files.createDirectories(inputDir);
			Files.createDirectories(outputDir);
			Files.createDirectories(logDir);
		} catch (IOException ex) {
			throw new RuntimeException("prepare task directories failed", ex);
		}
	}

	public void cleanupTaskDirs(Long taskId) {
		// reserved for future cleanup policy
	}

	private Path resolveTaskRoot(ExecutionContext context) {
		if (context != null && context.getInputFiles() != null) {
			for (InputFileMeta inputFile : context.getInputFiles()) {
				if (inputFile == null || inputFile.getUnpackDir() == null || inputFile.getUnpackDir().isBlank()) {
					continue;
				}
				Path unpackDir = Path.of(pathMappingSupport.toLinuxPath(inputFile.getUnpackDir())).normalize();
				Path taskRoot = unpackDir.getParent();
				return taskRoot == null ? unpackDir : taskRoot;
			}
		}
		return Path.of(nodeAgentConfig.getWorkRoot(), String.valueOf(context.getTaskId())).normalize();
	}

	private String normalize(Path path) {
		return path.toString().replace("\\", "/");
	}
}
