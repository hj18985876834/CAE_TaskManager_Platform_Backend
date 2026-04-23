package com.example.cae.task.interfaces.response;

public class TaskSubmitResponse {
	private Long taskId;
	private String status;
	private java.time.LocalDateTime submitTime;

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public java.time.LocalDateTime getSubmitTime() {
		return submitTime;
	}

	public void setSubmitTime(java.time.LocalDateTime submitTime) {
		this.submitTime = submitTime;
	}
}
