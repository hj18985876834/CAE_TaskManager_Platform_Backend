package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class TaskNodeMarkRequest {
	@NotNull(message = "nodeId不能为空")
	@Positive(message = "nodeId必须大于0")
	private Long nodeId;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}
}
