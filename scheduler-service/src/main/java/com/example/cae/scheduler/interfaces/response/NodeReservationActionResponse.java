package com.example.cae.scheduler.interfaces.response;

public class NodeReservationActionResponse {
	private Long taskId;
	private Long nodeId;
	private String reservationStatus;
	private Integer reservedCount;

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

	public String getReservationStatus() {
		return reservationStatus;
	}

	public void setReservationStatus(String reservationStatus) {
		this.reservationStatus = reservationStatus;
	}

	public Integer getReservedCount() {
		return reservedCount;
	}

	public void setReservedCount(Integer reservedCount) {
		this.reservedCount = reservedCount;
	}
}
