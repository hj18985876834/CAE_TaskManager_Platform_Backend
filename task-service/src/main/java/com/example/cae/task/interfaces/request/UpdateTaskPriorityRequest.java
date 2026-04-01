package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateTaskPriorityRequest {
	@NotNull(message = "priority is required")
	@Min(value = 0, message = "priority must be greater than or equal to 0")
	private Integer priority;

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}
}
