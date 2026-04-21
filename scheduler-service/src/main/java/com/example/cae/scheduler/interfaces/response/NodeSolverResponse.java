package com.example.cae.scheduler.interfaces.response;

public class NodeSolverResponse {
	private Long solverId;
	private String solverName;
	private String solverVersion;
	private Integer enabled;

	public Long getSolverId() {
		return solverId;
	}

	public void setSolverId(Long solverId) {
		this.solverId = solverId;
	}

	public String getSolverName() {
		return solverName;
	}

	public void setSolverName(String solverName) {
		this.solverName = solverName;
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
}
