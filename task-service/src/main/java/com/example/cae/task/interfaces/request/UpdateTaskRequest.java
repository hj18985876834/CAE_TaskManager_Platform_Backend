package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class UpdateTaskRequest {
	@Size(max = 100, message = "taskName长度不能超过100")
	private String taskName;
	@Min(value = 0, message = "priority不能小于0")
	private Integer priority;
	private Map<String, Object> params;

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}
}
