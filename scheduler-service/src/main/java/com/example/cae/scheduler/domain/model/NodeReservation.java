package com.example.cae.scheduler.domain.model;

import java.time.LocalDateTime;

public class NodeReservation {
	private Long id;
	private Long nodeId;
	private Long taskId;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime releasedAt;

	public boolean isReserved() {
		return "RESERVED".equals(status);
	}

	public void markReserved() {
		this.status = "RESERVED";
		this.releasedAt = null;
	}

	public void markReleased() {
		this.status = "RELEASED";
		this.releasedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public LocalDateTime getReleasedAt() {
		return releasedAt;
	}

	public void setReleasedAt(LocalDateTime releasedAt) {
		this.releasedAt = releasedAt;
	}
}
