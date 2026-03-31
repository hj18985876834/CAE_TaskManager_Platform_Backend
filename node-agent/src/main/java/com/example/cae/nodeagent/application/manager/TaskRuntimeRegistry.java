package com.example.cae.nodeagent.application.manager;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TaskRuntimeRegistry {
	private final ConcurrentMap<Long, Boolean> activeTasks = new ConcurrentHashMap<>();

	public boolean register(Long taskId) {
		if (taskId == null) {
			return false;
		}
		return activeTasks.putIfAbsent(taskId, Boolean.TRUE) == null;
	}

	public void finish(Long taskId) {
		if (taskId != null) {
			activeTasks.remove(taskId);
		}
	}

	public boolean isRunning(Long taskId) {
		return taskId != null && activeTasks.containsKey(taskId);
	}

	public int runningCount() {
		return activeTasks.size();
	}
}
