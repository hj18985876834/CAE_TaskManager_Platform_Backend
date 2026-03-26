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

