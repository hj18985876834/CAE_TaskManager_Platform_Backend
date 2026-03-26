package com.example.cae.nodeagent.interfaces.request;

public class CancelTaskRequest {
	private Long taskId;
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