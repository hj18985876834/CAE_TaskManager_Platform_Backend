package com.example.cae.user.application.assembler;

import com.example.cae.user.domain.model.Role;
import com.example.cae.user.domain.model.User;
import com.example.cae.user.interfaces.response.LoginResponse;
import com.example.cae.user.interfaces.response.UserDetailResponse;

public class UserAssembler {
	private UserAssembler() {
	}

	public static LoginResponse toLoginResponse(User user, Role role, String token) {
		LoginResponse response = new LoginResponse();
		response.setToken(token);
		response.setUserId(user.getId());
		response.setUsername(user.getUsername());
		response.setRoleCode(role.getRoleCode());
		return response;
	}

	public static UserDetailResponse toUserDetailResponse(User user, Role role) {
		UserDetailResponse response = new UserDetailResponse();
		response.setUserId(user.getId());
		response.setUsername(user.getUsername());
		response.setRealName(user.getRealName());
		response.setStatus(user.getStatus());
		response.setRoleCode(role.getRoleCode());
		response.setRoleName(role.getRoleName());
		return response;
	}
}

