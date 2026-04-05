package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InternalTaskFailRequest {
	@NotBlank(message = "failType不能为空")
	@Size(max = 32, message = "failType长度不能超过32")
	private String failType;
	@Size(max = 255, message = "reason长度不能超过255")
	private String reason;

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
}
