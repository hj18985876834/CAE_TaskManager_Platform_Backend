package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class StatusReportRequest {
	@NotNull(message = "nodeId不能为空")
	@Positive(message = "nodeId必须大于0")
	private Long nodeId;
	@Size(max = 30, message = "fromStatus长度不能超过30")
	private String fromStatus;
	@Size(max = 30, message = "toStatus长度不能超过30")
	private String toStatus;
	@Size(max = 255, message = "changeReason长度不能超过255")
	private String changeReason;
	@Size(max = 30, message = "operatorType长度不能超过30")
	private String operatorType;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getFromStatus() {
		return fromStatus;
	}

	public void setFromStatus(String fromStatus) {
		this.fromStatus = fromStatus;
	}

	public String getToStatus() {
		return toStatus;
	}

	public void setToStatus(String toStatus) {
		this.toStatus = toStatus;
	}

	public String getChangeReason() {
		return changeReason;
	}

	public void setChangeReason(String changeReason) {
		this.changeReason = changeReason;
	}

	public String getOperatorType() {
		return operatorType;
	}

	public void setOperatorType(String operatorType) {
		this.operatorType = operatorType;
	}

	@AssertTrue(message = "toStatus不能为空")
	public boolean isTargetStatusPresent() {
		return toStatus != null && !toStatus.isBlank();
	}

	@AssertTrue(message = "status-report首版只允许回传RUNNING")
	public boolean isRunningOnly() {
		String target = toStatus;
		return target == null || "RUNNING".equalsIgnoreCase(target.trim());
	}
}
