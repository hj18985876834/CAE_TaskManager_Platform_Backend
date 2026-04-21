package com.example.cae.task.interfaces.response;

import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;

public class TaskDetailResponse {
	private Long taskId;
	private String taskNo;
	private String taskName;
	private Long userId;
	private Long solverId;
	private String solverName;
	private Long profileId;
	private String profileName;
	private String taskType;
	private String status;
	private Integer priority;
	private Long nodeId;
	private String nodeName;
	private Boolean canCancel;
	private Boolean canRetry;
	private String queueReason;
	private Map<String, Object> params;
	private String failType;
	private String failMessage;
	private List<TaskStatusHistoryResponse> statusHistory;
	private List<TaskScheduleRecordResponse> scheduleRecords;
	private LocalDateTime submitTime;
	private LocalDateTime startTime;
	private LocalDateTime endTime;

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
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

	public String getSolverName() {
		return solverName;
	}

	public void setSolverName(String solverName) {
		this.solverName = solverName;
	}

	public Long getProfileId() {
		return profileId;
	}

	public void setProfileId(Long profileId) {
		this.profileId = profileId;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
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

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public Boolean getCanCancel() {
		return canCancel;
	}

	public void setCanCancel(Boolean canCancel) {
		this.canCancel = canCancel;
	}

	public Boolean getCanRetry() {
		return canRetry;
	}

	public void setCanRetry(Boolean canRetry) {
		this.canRetry = canRetry;
	}

	public String getQueueReason() {
		return queueReason;
	}

	public void setQueueReason(String queueReason) {
		this.queueReason = queueReason;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
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

	public List<TaskStatusHistoryResponse> getStatusHistory() {
		return statusHistory;
	}

	public void setStatusHistory(List<TaskStatusHistoryResponse> statusHistory) {
		this.statusHistory = statusHistory;
	}

	public List<TaskScheduleRecordResponse> getScheduleRecords() {
		return scheduleRecords;
	}

	public void setScheduleRecords(List<TaskScheduleRecordResponse> scheduleRecords) {
		this.scheduleRecords = scheduleRecords;
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
}
