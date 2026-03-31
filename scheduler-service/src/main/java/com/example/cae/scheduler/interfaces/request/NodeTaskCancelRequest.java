package com.example.cae.scheduler.interfaces.request;

public class NodeTaskCancelRequest {
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
