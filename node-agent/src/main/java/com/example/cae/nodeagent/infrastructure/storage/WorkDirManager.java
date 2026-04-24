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
		Path executionDir = resolveValidatedExecutionDir(context);
		validateValidatedExecutionDir(context, executionDir);
		Path root = resolveTaskRoot(context, executionDir);
		Path workDir = root.resolve("workdir");
		Path inputDir = root.resolve("input");
		Path outputDir = root.resolve("output");
		Path logDir = root.resolve("logs");
		Path metaDir = root.resolve("meta");

		context.setWorkDir(normalize(workDir));
		context.setTaskDir(normalize(executionDir == null ? workDir : executionDir));
		context.setInputDir(normalize(inputDir));
		context.setOutputDir(normalize(outputDir));
		context.setLogDir(normalize(logDir));

		try {
			Files.createDirectories(workDir);
			Files.createDirectories(inputDir);
			Files.createDirectories(outputDir);
			Files.createDirectories(logDir);
			Files.createDirectories(metaDir);
		} catch (IOException ex) {
			throw new RuntimeException("prepare task directories failed", ex);
		}
	}

	public void cleanupTaskDirs(Long taskId) {
		// reserved for future cleanup policy
	}

	private Path resolveTaskRoot(ExecutionContext context, Path executionDir) {
		if (executionDir != null) {
			Path current = executionDir;
			while (current != null) {
				Path fileName = current.getFileName();
				if (fileName != null && "workdir".equals(fileName.toString())) {
					Path taskRoot = current.getParent();
					return taskRoot == null ? current : taskRoot;
				}
				current = current.getParent();
			}
			Path taskRoot = executionDir.getParent();
			return taskRoot == null ? executionDir : taskRoot;
		}
		return Path.of(nodeAgentConfig.getWorkRoot(), String.valueOf(context.getTaskId())).normalize();
	}

	private Path resolveValidatedExecutionDir(ExecutionContext context) {
		if (context == null || context.getInputFiles() == null) {
			return null;
		}
		Path validatedExecutionDir = null;
		for (InputFileMeta inputFile : context.getInputFiles()) {
			if (inputFile == null || inputFile.getUnpackDir() == null || inputFile.getUnpackDir().isBlank()) {
				continue;
			}
			Path candidate = Path.of(pathMappingSupport.toLinuxPath(inputFile.getUnpackDir())).normalize();
			if (validatedExecutionDir == null) {
				validatedExecutionDir = candidate;
			} else if (!validatedExecutionDir.equals(candidate)) {
				throw new IllegalStateException("multiple unpackDir values found for taskId=" + context.getTaskId());
			}
		}
		return validatedExecutionDir;
	}

	private void validateValidatedExecutionDir(ExecutionContext context, Path executionDir) {
		if (executionDir == null) {
			return;
		}
		if (!Files.exists(executionDir)) {
			throw new IllegalStateException("validated unpackDir not found before directory preparation, taskId="
					+ (context == null ? null : context.getTaskId())
					+ ", mappedPath=" + executionDir);
		}
		if (!Files.isDirectory(executionDir)) {
			throw new IllegalStateException("validated unpackDir is not a directory before directory preparation, taskId="
					+ (context == null ? null : context.getTaskId())
					+ ", mappedPath=" + executionDir);
		}
	}

	private String normalize(Path path) {
		return path.toString().replace("\\", "/");
	}
}
