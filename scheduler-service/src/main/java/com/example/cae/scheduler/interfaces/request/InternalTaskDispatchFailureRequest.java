package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class InternalTaskDispatchFailureRequest {
	@NotNull(message = "nodeId不能为空")
	@Positive(message = "nodeId必须大于0")
	private Long nodeId;
	@NotBlank(message = "failType不能为空")
	@Size(max = 32, message = "failType长度不能超过32")
	private String failType;
	@Size(max = 255, message = "reason长度不能超过255")
	private String reason;
	@NotNull(message = "recoverable不能为空")
	private Boolean recoverable;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getFailType() {
		return failType;
	}

	public void setFailType(String failType) {
		this.failType = failType;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Boolean getRecoverable() {
		return recoverable;
	}

	public void setRecoverable(Boolean recoverable) {
		this.recoverable = recoverable;
	}
}
