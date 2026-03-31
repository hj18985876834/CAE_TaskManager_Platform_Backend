package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class NodeAgentRegisterRequest {
	@NotBlank(message = "nodeCode不能为空")
	@Size(max = 64, message = "nodeCode长度不能超过64")
	private String nodeCode;
	@NotBlank(message = "nodeName不能为空")
	@Size(max = 128, message = "nodeName长度不能超过128")
	private String nodeName;
	@Size(max = 128, message = "host长度不能超过128")
	private String host;
	@NotNull(message = "maxConcurrency不能为空")
	@Positive(message = "maxConcurrency必须大于0")
	private Integer maxConcurrency;
	@NotEmpty(message = "solvers不能为空")
	@Valid
	private List<SolverItem> solvers;

	public String getNodeCode() {
		return nodeCode;
	}

	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(Integer maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public List<SolverItem> getSolvers() {
		return solvers;
	}

	public void setSolvers(List<SolverItem> solvers) {
		this.solvers = solvers;
	}

	public static class SolverItem {
		@NotNull(message = "solverId不能为空")
		@Positive(message = "solverId必须大于0")
		private Long solverId;
		@NotBlank(message = "solverVersion不能为空")
		@Size(max = 64, message = "solverVersion长度不能超过64")
		private String solverVersion;

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
	}
}
