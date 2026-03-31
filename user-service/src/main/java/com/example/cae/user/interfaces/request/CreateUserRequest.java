package com.example.cae.user.interfaces.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {
	@NotBlank(message = "username不能为空")
	@Size(max = 64, message = "username长度不能超过64")
	private String username;
	@NotBlank(message = "password不能为空")
	@Size(min = 6, max = 64, message = "password长度必须在6到64之间")
	private String password;
	@NotBlank(message = "realName不能为空")
	@Size(max = 64, message = "realName长度不能超过64")
	private String realName;
	@NotNull(message = "roleId不能为空")
	@Positive(message = "roleId必须大于0")
	private Long roleId;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

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
