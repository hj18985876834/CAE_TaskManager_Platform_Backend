package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.constraints.NotBlank;

public class UpdateNodeStatusRequest {
	@NotBlank(message = "status不能为空")
	private String status;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
