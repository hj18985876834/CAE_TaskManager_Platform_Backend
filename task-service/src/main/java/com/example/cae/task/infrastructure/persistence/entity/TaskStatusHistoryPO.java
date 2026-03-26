package com.example.cae.task.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class TaskStatusHistoryPO {
	private Long id;
	private Long taskId;
	private String fromStatus;
	private String toStatus;
	private String changeReason;
	private String operatorType;
	private Long operatorId;
	private LocalDateTime createdAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getTaskId() { return taskId; }
	public void setTaskId(Long taskId) { this.taskId = taskId; }
	public String getFromStatus() { return fromStatus; }
	public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
	public String getToStatus() { return toStatus; }
	public void setToStatus(String toStatus) { this.toStatus = toStatus; }
	public String getChangeReason() { return changeReason; }
	public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
	public String getOperatorType() { return operatorType; }
	public void setOperatorType(String operatorType) { this.operatorType = operatorType; }
	public Long getOperatorId() { return operatorId; }
	public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

