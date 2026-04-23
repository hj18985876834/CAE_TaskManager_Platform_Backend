package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class MyTaskListQueryRequest {
	@Min(value = 1, message = "pageNum必须大于等于1")
	private Integer pageNum;
	@Min(value = 1, message = "pageSize必须大于等于1")
	@Max(value = 200, message = "pageSize不能超过200")
	private Integer pageSize;
	private String taskName;
	private String status;
	@Min(value = 0, message = "priority must be greater than or equal to 0")
	private Integer priority;
	@Positive(message = "solverId必须大于0")
	private Long solverId;
	private String taskType;
	private LocalDateTime startTime;
	private LocalDateTime endTime;

	public Integer getPageNum() { return pageNum; }
	public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
	public Integer getPageSize() { return pageSize; }
	public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
	public String getTaskName() { return taskName; }
	public void setTaskName(String taskName) { this.taskName = taskName; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public Integer getPriority() { return priority; }
	public void setPriority(Integer priority) { this.priority = priority; }
	public Long getSolverId() { return solverId; }
	public void setSolverId(Long solverId) { this.solverId = solverId; }
	public String getTaskType() { return taskType; }
	public void setTaskType(String taskType) { this.taskType = taskType; }
	public LocalDateTime getStartTime() { return startTime; }
	public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
	public LocalDateTime getEndTime() { return endTime; }
	public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
