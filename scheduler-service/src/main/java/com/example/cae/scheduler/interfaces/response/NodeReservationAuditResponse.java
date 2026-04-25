package com.example.cae.scheduler.interfaces.response;

import java.time.LocalDateTime;
import java.util.List;

public class NodeReservationAuditResponse {
	private Long nodeId;
	private Integer reservedCount;
	private Integer reservedDetailCount;
	private Boolean consistent;
	private List<NodeReservationAuditItemResponse> issues;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public Integer getReservedCount() {
		return reservedCount;
	}

	public void setReservedCount(Integer reservedCount) {
		this.reservedCount = reservedCount;
	}

	public Integer getReservedDetailCount() {
		return reservedDetailCount;
	}

	public void setReservedDetailCount(Integer reservedDetailCount) {
		this.reservedDetailCount = reservedDetailCount;
	}

	public Boolean getConsistent() {
		return consistent;
	}

	public void setConsistent(Boolean consistent) {
		this.consistent = consistent;
	}

	public List<NodeReservationAuditItemResponse> getIssues() {
		return issues;
	}

	public void setIssues(List<NodeReservationAuditItemResponse> issues) {
		this.issues = issues;
	}

	public static class NodeReservationAuditItemResponse {
		private Long reservationId;
		private Long taskId;
		private Long reservationNodeId;
		private String reservationStatus;
		private String taskStatus;
		private Long taskNodeId;
		private String issueType;
		private String message;
		private LocalDateTime updatedAt;

		public Long getReservationId() {
			return reservationId;
		}

		public void setReservationId(Long reservationId) {
			this.reservationId = reservationId;
		}

		public Long getTaskId() {
			return taskId;
		}

		public void setTaskId(Long taskId) {
			this.taskId = taskId;
		}

		public Long getReservationNodeId() {
			return reservationNodeId;
		}

		public void setReservationNodeId(Long reservationNodeId) {
			this.reservationNodeId = reservationNodeId;
		}

		public String getReservationStatus() {
			return reservationStatus;
		}

		public void setReservationStatus(String reservationStatus) {
			this.reservationStatus = reservationStatus;
		}

		public String getTaskStatus() {
			return taskStatus;
		}

		public void setTaskStatus(String taskStatus) {
			this.taskStatus = taskStatus;
		}

		public Long getTaskNodeId() {
			return taskNodeId;
		}

		public void setTaskNodeId(Long taskNodeId) {
			this.taskNodeId = taskNodeId;
		}

		public String getIssueType() {
			return issueType;
		}

		public void setIssueType(String issueType) {
			this.issueType = issueType;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public LocalDateTime getUpdatedAt() {
			return updatedAt;
		}

		public void setUpdatedAt(LocalDateTime updatedAt) {
			this.updatedAt = updatedAt;
		}
	}
}
