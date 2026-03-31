package com.example.cae.user.interfaces.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {
	@Size(max = 64, message = "realName长度不能超过64")
	private String realName;
	@Positive(message = "roleId必须大于0")
	private Long roleId;

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}
}
