package com.example.cae.task.interfaces.request;

import com.example.cae.common.constant.QueryValidationConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class AdminTaskListQueryRequest {
	@Min(value = 1, message = "pageNum必须大于等于1")
	private Integer pageNum;
	@Min(value = 1, message = "pageSize必须大于等于1")
	@Max(value = 200, message = "pageSize不能超过200")
	private Integer pageSize;
	@Size(max = QueryValidationConstants.TASK_NAME_MAX_LENGTH, message = "taskName长度不能超过100")
	private String taskName;
	@Size(max = QueryValidationConstants.STATUS_MAX_LENGTH, message = "status长度不能超过30")
	private String status;
	@Min(value = 0, message = "priority must be greater than or equal to 0")
	private Integer priority;
	@Positive(message = "solverId必须大于0")
	private Long solverId;
	@Size(max = QueryValidationConstants.TASK_TYPE_MAX_LENGTH, message = "taskType长度不能超过50")
	private String taskType;
	@Positive(message = "userId必须大于0")
	private Long userId;
	@Positive(message = "nodeId必须大于0")
	private Long nodeId;
	@Size(max = QueryValidationConstants.FAIL_TYPE_MAX_LENGTH, message = "failType长度不能超过32")
	private String failType;
	@DateTimeFormat(pattern = QueryValidationConstants.STANDARD_DATE_TIME_PATTERN)
	private LocalDateTime startTime;
	@DateTimeFormat(pattern = QueryValidationConstants.STANDARD_DATE_TIME_PATTERN)
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
	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }
	public Long getNodeId() { return nodeId; }
	public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
	public String getFailType() { return failType; }
	public void setFailType(String failType) { this.failType = failType; }
	public LocalDateTime getStartTime() { return startTime; }
	public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
	public LocalDateTime getEndTime() { return endTime; }
	public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
