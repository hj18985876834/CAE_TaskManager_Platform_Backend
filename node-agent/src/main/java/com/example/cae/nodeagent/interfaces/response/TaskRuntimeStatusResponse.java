package com.example.cae.nodeagent.interfaces.response;

public class TaskRuntimeStatusResponse {
	private Long taskId;
	private Boolean active;
	private Boolean runningReported;

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Boolean getRunningReported() {
		return runningReported;
	}

	public void setRunningReported(Boolean runningReported) {
		this.runningReported = runningReported;
	}
}
