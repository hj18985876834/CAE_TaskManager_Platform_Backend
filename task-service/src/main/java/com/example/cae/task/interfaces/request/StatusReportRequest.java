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
	@Size(max = 30, message = "status长度不能超过30")
	private String status;
	@Size(max = 255, message = "reason长度不能超过255")
	private String reason;

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
		if (this.toStatus == null || this.toStatus.isBlank()) {
			this.toStatus = status;
		}
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
		if (this.changeReason == null || this.changeReason.isBlank()) {
			this.changeReason = reason;
		}
	}

	@AssertTrue(message = "toStatus或status至少提供一个")
	public boolean isTargetStatusPresent() {
		return (toStatus != null && !toStatus.isBlank()) || (status != null && !status.isBlank());
	}

	@AssertTrue(message = "status-report首版只允许回传RUNNING")
	public boolean isRunningOnly() {
		String target = null;
		if (toStatus != null && !toStatus.isBlank()) {
			target = toStatus;
		} else if (status != null && !status.isBlank()) {
			target = status;
		}
		return target == null || "RUNNING".equalsIgnoreCase(target.trim());
	}
}
