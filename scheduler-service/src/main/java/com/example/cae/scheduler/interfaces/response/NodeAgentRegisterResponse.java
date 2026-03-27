package com.example.cae.scheduler.interfaces.response;

public class NodeAgentRegisterResponse {
	private Long nodeId;
	private String nodeToken;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getNodeToken() {
		return nodeToken;
	}

	public void setNodeToken(String nodeToken) {
		this.nodeToken = nodeToken;
	}
}