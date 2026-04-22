package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

import java.util.Set;

public class MarkFinishedRequest {
	@NotNull(message = "nodeId不能为空")
	@Positive(message = "nodeId必须大于0")
	private Long nodeId;
	@NotBlank(message = "finalStatus不能为空")
	@Size(max = 30, message = "finalStatus长度不能超过30")
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

	@AssertTrue(message = "finalStatus只允许为SUCCESS或TIMEOUT")
	public boolean isFinalStatusAllowed() {
		if (finalStatus == null || finalStatus.isBlank()) {
			return true;
		}
		return Set.of("SUCCESS", "TIMEOUT").contains(finalStatus.trim().toUpperCase());
	}
}
