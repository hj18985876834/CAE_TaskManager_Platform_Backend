package com.example.cae.task.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cae.storage")
public class TaskStorageProperties {
	private String taskRoot = "./data/tasks";

	public String getTaskRoot() {
		return taskRoot;
	}

	public void setTaskRoot(String taskRoot) {
		this.taskRoot = taskRoot;
	}
}
