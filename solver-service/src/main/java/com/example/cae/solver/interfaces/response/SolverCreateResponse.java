package com.example.cae.solver.interfaces.response;

public class SolverCreateResponse {
	private Long solverId;
	private String solverCode;
	private String solverName;
	private Integer enabled;

	public Long getSolverId() {
		return solverId;
	}

	public void setSolverId(Long solverId) {
		this.solverId = solverId;
	}

	public String getSolverCode() {
		return solverCode;
	}

	public void setSolverCode(String solverCode) {
		this.solverCode = solverCode;
	}

	public String getSolverName() {
		return solverName;
	}

	public void setSolverName(String solverName) {
		this.solverName = solverName;
	}

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}
}
