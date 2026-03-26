package com.example.cae.scheduler.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class NodeSolverCapabilityPO {
	private Long id;
	private Long nodeId;
	private Long solverId;
	private String solverVersion;
	private Integer enabled;
	private LocalDateTime createdAt;

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

	public Long getSolverId() {
		return solverId;
	}

	public void setSolverId(Long solverId) {
		this.solverId = solverId;
	}

	public String getSolverVersion() {
		return solverVersion;
	}

	public void setSolverVersion(String solverVersion) {
		this.solverVersion = solverVersion;
	}

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
