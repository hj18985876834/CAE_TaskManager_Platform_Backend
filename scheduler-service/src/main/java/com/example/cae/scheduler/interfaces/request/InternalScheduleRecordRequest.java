package com.example.cae.scheduler.interfaces.request;

public class InternalScheduleRecordRequest {
	private Long taskId;
	private Long nodeId;
	private String strategyName;
	private String scheduleStatus;
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
