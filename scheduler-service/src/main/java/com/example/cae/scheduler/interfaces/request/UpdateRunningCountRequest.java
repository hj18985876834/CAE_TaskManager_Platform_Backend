package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.constraints.NotNull;

public class UpdateRunningCountRequest {
	@NotNull(message = "delta不能为空")
	private Integer delta;

	public Integer getDelta() {
		return delta;
	}

	public void setDelta(Integer delta) {
		this.delta = delta;
	}
}
