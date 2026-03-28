package com.example.cae.task.interfaces.request;

public class MarkFinishedRequest {
	private Long nodeId;
	private String finalStatus;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getFinalStatus() {
		return finalStatus;
	}

	public void setFinalStatus(String finalStatus) {
		this.finalStatus = finalStatus;
	}
}
