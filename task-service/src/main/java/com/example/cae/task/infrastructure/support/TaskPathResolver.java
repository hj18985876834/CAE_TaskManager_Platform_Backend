package com.example.cae.task.infrastructure.support;

import org.springframework.stereotype.Component;

@Component
public class TaskPathResolver {
	public String resolveTaskRoot(Long taskId) {
		return "data/tasks/" + taskId;
	}

	public String resolveInputDir(Long taskId) {
		return resolveTaskRoot(taskId) + "/input";
	}

	public String resolveInputRoleDir(Long taskId, String fileRole) {
		String suffix = fileRole == null || fileRole.isBlank() ? "input" : fileRole.toLowerCase();
		return resolveTaskRoot(taskId) + "/input/" + suffix;
	}

	public String resolveLogDir(Long taskId) {
		return resolveTaskRoot(taskId) + "/log";
	}

	public String resolveResultDir(Long taskId) {
		return resolveTaskRoot(taskId) + "/result";
	}

	public String resolveLogFile(Long taskId) {
		return resolveLogDir(taskId) + "/task.log";
	}
}
