package com.example.cae.user.application.assembler;

import com.example.cae.user.domain.model.Role;
import com.example.cae.user.domain.model.User;
import com.example.cae.user.interfaces.request.CreateUserRequest;
import com.example.cae.user.interfaces.response.CurrentUserResponse;
import com.example.cae.user.interfaces.response.LoginResponse;
import com.example.cae.user.interfaces.response.UserDetailResponse;
import com.example.cae.user.interfaces.response.UserListItemResponse;

public class UserAssembler {
	private UserAssembler() {
	}

	public static LoginResponse toLoginResponse(User user, Role role, String token) {
		LoginResponse response = new LoginResponse();
		response.setToken(token);
		response.setTokenType("Bearer");
		response.setUserId(user.getId());
		response.setUsername(user.getUsername());
		response.setRoleCode(role.getRoleCode());
		return response;
	}

	public static CurrentUserResponse toCurrentUserResponse(User user, String roleCode) {
		CurrentUserResponse response = new CurrentUserResponse();
		response.setUserId(user.getId());
		response.setUsername(user.getUsername());
		response.setRealName(user.getRealName());
		response.setRoleCode(roleCode);
		return response;
	}

	public static UserDetailResponse toUserDetailResponse(User user, Role role) {
		UserDetailResponse response = new UserDetailResponse();
		response.setUserId(user.getId());
		response.setUsername(user.getUsername());
		response.setRealName(user.getRealName());
		response.setRoleId(user.getRoleId());
		response.setStatus(user.getStatus());
		response.setRoleCode(role.getRoleCode());
		response.setRoleName(role.getRoleName());
		return response;
	}

	public static UserListItemResponse toUserListItem(User user, String roleCode) {
		UserListItemResponse response = new UserListItemResponse();
		response.setUserId(user.getId());
		response.setUsername(user.getUsername());
		response.setRealName(user.getRealName());
		response.setRoleId(user.getRoleId());
		response.setRoleCode(roleCode);
		response.setStatus(user.getStatus());
		return response;
	}

	public static User toDomain(CreateUserRequest request) {
		User user = new User();
		user.setUsername(request.getUsername());
		user.setRealName(request.getRealName());
		user.setRoleId(request.getRoleId());
		return user;
	}
}

