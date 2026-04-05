package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateNodeSolverStatusRequest {
	@NotNull(message = "enabled不能为空")
	@Min(value = 0, message = "enabled只能是0或1")
	@Max(value = 1, message = "enabled只能是0或1")
	private Integer enabled;

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}
}
