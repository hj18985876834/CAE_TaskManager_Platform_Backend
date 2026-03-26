package com.example.cae.user.interfaces.request;

public class UpdateUserRequest {
	private String realName;
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

