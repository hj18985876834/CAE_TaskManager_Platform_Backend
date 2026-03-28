package com.example.cae.task.interfaces.request;

public class StatusReportRequest {
	private Long nodeId;
	private String fromStatus;
	private String toStatus;
	private String changeReason;
	private String operatorType;
	private String status;
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
}

