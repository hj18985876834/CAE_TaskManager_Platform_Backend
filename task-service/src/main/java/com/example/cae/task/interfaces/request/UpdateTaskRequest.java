package com.example.cae.task.interfaces.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class UpdateTaskRequest {
	@Size(max = 128, message = "taskName长度不能超过128")
	private String taskName;
	@Min(value = 0, message = "priority不能小于0")
	private Integer priority;
	@JsonAlias("paramsJson")
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
