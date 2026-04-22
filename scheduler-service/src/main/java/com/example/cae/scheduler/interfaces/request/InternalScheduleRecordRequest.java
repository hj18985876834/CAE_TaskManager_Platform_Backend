package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class InternalScheduleRecordRequest {
	@NotNull(message = "taskId不能为空")
	@Positive(message = "taskId必须大于0")
	private Long taskId;
	@Positive(message = "nodeId必须大于0")
	private Long nodeId;
	@Size(max = 50, message = "strategyName长度不能超过50")
	private String strategyName;
	@Size(max = 30, message = "scheduleStatus长度不能超过30")
	private String scheduleStatus;
	@Size(max = 255, message = "scheduleMessage长度不能超过255")
	private String scheduleMessage;

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

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}

	public String getScheduleStatus() {
		return scheduleStatus;
	}

	public void setScheduleStatus(String scheduleStatus) {
		this.scheduleStatus = scheduleStatus;
	}

	public String getScheduleMessage() {
		return scheduleMessage;
	}

	public void setScheduleMessage(String scheduleMessage) {
		this.scheduleMessage = scheduleMessage;
	}
}
