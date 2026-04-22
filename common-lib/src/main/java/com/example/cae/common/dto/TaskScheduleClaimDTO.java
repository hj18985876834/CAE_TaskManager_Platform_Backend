package com.example.cae.common.dto;

public class TaskScheduleClaimDTO {
	private Boolean claimed;
	private Long taskId;
	private Long nodeId;
	private String status;

	public Boolean getClaimed() {
		return claimed;
	}

	public void setClaimed(Boolean claimed) {
		this.claimed = claimed;
	}

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
