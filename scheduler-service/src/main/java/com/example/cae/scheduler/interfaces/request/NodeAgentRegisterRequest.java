package com.example.cae.scheduler.interfaces.request;

import java.util.List;

public class NodeAgentRegisterRequest {
	private String nodeCode;
	private String nodeName;
	private String host;
	private Integer port;
	private Integer maxConcurrency;
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

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
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
		private Long solverId;
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