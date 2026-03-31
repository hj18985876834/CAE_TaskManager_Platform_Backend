package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class NodeTaskCancelRequest {
	@NotNull(message = "taskId不能为空")
	@Positive(message = "taskId必须大于0")
	private Long taskId;
	@Size(max = 255, message = "reason长度不能超过255")
	private String reason;

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
