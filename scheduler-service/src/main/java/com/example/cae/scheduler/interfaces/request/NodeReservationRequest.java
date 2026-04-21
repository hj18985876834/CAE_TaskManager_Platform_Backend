package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class NodeReservationRequest {
	@NotNull(message = "taskId不能为空")
	@Positive(message = "taskId必须大于0")
	private Long taskId;

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}
}
