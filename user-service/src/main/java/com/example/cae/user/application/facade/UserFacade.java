package com.example.cae.user.application.facade;

import com.example.cae.common.response.PageResult;
import com.example.cae.user.application.service.UserAppService;
import com.example.cae.user.interfaces.request.CreateUserRequest;
import com.example.cae.user.interfaces.request.ResetPasswordRequest;
import com.example.cae.user.interfaces.request.UpdateUserRequest;
import com.example.cae.user.interfaces.request.UpdateUserStatusRequest;
import com.example.cae.user.interfaces.request.UserPageQueryRequest;
import com.example.cae.user.interfaces.response.InternalUserBasicResponse;
import com.example.cae.user.interfaces.response.UserCreateResponse;
import com.example.cae.user.interfaces.response.UserDetailResponse;
import com.example.cae.user.interfaces.response.UserListItemResponse;
import org.springframework.stereotype.Component;

@Component
public class UserFacade {
	private final UserAppService userAppService;

	public UserFacade(UserAppService userAppService) {
		this.userAppService = userAppService;
	}

	public PageResult<UserListItemResponse> pageUsers(UserPageQueryRequest request) {
		return userAppService.pageUsers(request);
	}

	public UserCreateResponse createUser(CreateUserRequest request) {
		return userAppService.createUser(request);
	}

	public UserDetailResponse getById(Long userId) {
		return userAppService.getById(userId);
	}

	public void updateUser(Long userId, UpdateUserRequest request) {
		userAppService.updateUser(userId, request);
	}

	public void updateStatus(Long userId, UpdateUserStatusRequest request) {
		userAppService.updateStatus(userId, request);
	}

	public void resetPassword(Long userId, ResetPasswordRequest request) {
		userAppService.resetPassword(userId, request);
	}

	public InternalUserBasicResponse getInternalById(Long userId) {
		return userAppService.getInternalById(userId);
	}
}
