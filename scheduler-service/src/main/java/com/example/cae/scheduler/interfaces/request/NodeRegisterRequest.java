package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class NodeRegisterRequest {
	@NotBlank(message = "nodeCode不能为空")
	@Size(max = 64, message = "nodeCode长度不能超过64")
	private String nodeCode;
	@NotBlank(message = "nodeName不能为空")
	@Size(max = 128, message = "nodeName长度不能超过128")
	private String nodeName;
	@NotBlank(message = "host不能为空")
	@Size(max = 128, message = "host长度不能超过128")
	private String host;
	@NotNull(message = "port不能为空")
	@Positive(message = "port必须大于0")
	private Integer port;
	@NotNull(message = "maxConcurrency不能为空")
	@Positive(message = "maxConcurrency必须大于0")
	private Integer maxConcurrency;
	@NotEmpty(message = "solverIds不能为空")
	private List<Long> solverIds;

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

	public List<Long> getSolverIds() {
		return solverIds;
	}

	public void setSolverIds(List<Long> solverIds) {
		this.solverIds = solverIds;
	}
}
