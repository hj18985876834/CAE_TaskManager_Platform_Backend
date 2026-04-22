package com.example.cae.user.interfaces.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
	@NotBlank(message = "username不能为空")
	@Size(max = 50, message = "username长度不能超过50")
	private String username;
	@NotBlank(message = "password不能为空")
	@Size(max = 64, message = "password长度不能超过64")
	private String password;

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
}
