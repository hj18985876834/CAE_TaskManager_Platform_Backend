package com.example.cae.task.infrastructure.support;

import com.example.cae.task.config.TaskStorageProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class TaskPathResolver {
	private final TaskStorageProperties taskStorageProperties;

	public TaskPathResolver(TaskStorageProperties taskStorageProperties) {
		this.taskStorageProperties = taskStorageProperties;
	}

	public String resolveTaskRoot(Long taskId) {
		return resolvePath(taskStorageProperties.getTaskRoot(), String.valueOf(taskId));
	}

	public String resolveInputDir(Long taskId) {
		return resolvePath(resolveTaskRoot(taskId), "input");
	}

	public String resolveInputRoleDir(Long taskId, String fileRole) {
		String suffix = fileRole == null || fileRole.isBlank() ? "input" : fileRole.toLowerCase();
		return resolvePath(resolveTaskRoot(taskId), "input", suffix);
	}

	public String resolveLogDir(Long taskId) {
		return resolvePath(resolveTaskRoot(taskId), "log");
	}

	public String resolveResultDir(Long taskId) {
		return resolvePath(resolveTaskRoot(taskId), "result");
	}

	public String resolveLogFile(Long taskId) {
		return resolvePath(resolveLogDir(taskId), "task.log");
	}

	private String resolvePath(String first, String... more) {
		return Path.of(first, more).normalize().toString().replace("\\", "/");
	}
}
