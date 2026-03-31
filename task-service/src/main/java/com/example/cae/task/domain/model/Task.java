package com.example.cae.task.domain.model;

import com.example.cae.common.enums.TaskStatusEnum;

import java.time.LocalDateTime;

public class Task {
	private Long id;
	private String taskNo;
	private String taskName;
	private Long userId;
	private Long solverId;
	private Long profileId;
	private String taskType;
	private String status;
	private Integer priority;
	private Long nodeId;
	private String paramsJson;
	private LocalDateTime submitTime;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String failType;
	private String failMessage;
	private Integer deletedFlag;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public void markValidated() {
		this.status = TaskStatusEnum.VALIDATED.name();
	}

	public void submit() {
		this.status = TaskStatusEnum.QUEUED.name();
		this.submitTime = LocalDateTime.now();
	}

	public void bindNode(Long nodeId) {
		this.nodeId = nodeId;
	}

	public void markScheduled() {
		this.status = TaskStatusEnum.SCHEDULED.name();
	}

	public void markDispatched() {
		this.status = TaskStatusEnum.DISPATCHED.name();
	}

	public void markRunning() {
		this.status = TaskStatusEnum.RUNNING.name();
		this.startTime = LocalDateTime.now();
	}

	public void markSuccess() {
		this.status = TaskStatusEnum.SUCCESS.name();
		this.endTime = LocalDateTime.now();
	}

	public void markFailed(String failType, String failMessage) {
		this.status = TaskStatusEnum.FAILED.name();
		this.failType = failType;
		this.failMessage = failMessage;
		this.endTime = LocalDateTime.now();
	}

	public void cancel() {
		this.status = TaskStatusEnum.CANCELED.name();
		this.endTime = LocalDateTime.now();
	}

	public void markTimeout(String failMessage) {
		this.status = TaskStatusEnum.TIMEOUT.name();
		this.failType = TaskStatusEnum.TIMEOUT.name();
		this.failMessage = failMessage;
		this.endTime = LocalDateTime.now();
	}

	public boolean isOwner(Long userId) {
		return this.userId != null && this.userId.equals(userId);
	}

	public boolean isFinished() {
		if (status == null) {
			return false;
		}
		try {
			return TaskStatusEnum.valueOf(status).isFinished();
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTaskNo() {
		return taskNo;
	}

	public void setTaskNo(String taskNo) {
		this.taskNo = taskNo;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getSolverId() {
		return solverId;
	}

	public void setSolverId(Long solverId) {
		this.solverId = solverId;
	}

	public Long getProfileId() {
		return profileId;
	}

	public void setProfileId(Long profileId) {
		this.profileId = profileId;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getParamsJson() {
		return paramsJson;
	}

	public void setParamsJson(String paramsJson) {
		this.paramsJson = paramsJson;
	}

	public LocalDateTime getSubmitTime() {
		return submitTime;
	}

	public void setSubmitTime(LocalDateTime submitTime) {
		this.submitTime = submitTime;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public String getFailType() {
		return failType;
	}

	public void setFailType(String failType) {
		this.failType = failType;
	}

	public String getFailMessage() {
		return failMessage;
	}

	public void setFailMessage(String failMessage) {
		this.failMessage = failMessage;
	}

	public Integer getDeletedFlag() {
		return deletedFlag;
	}

	public void setDeletedFlag(Integer deletedFlag) {
		this.deletedFlag = deletedFlag;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
