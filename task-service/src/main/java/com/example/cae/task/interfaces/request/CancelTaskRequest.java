package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.Size;

public class CancelTaskRequest {
	@Size(max = 255, message = "reason长度不能超过255")
	private String reason;

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
