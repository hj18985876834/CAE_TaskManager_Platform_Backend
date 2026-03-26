package com.example.cae.user.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.common.response.PageResult;
import com.example.cae.user.application.facade.UserFacade;
import com.example.cae.user.interfaces.request.CreateUserRequest;
import com.example.cae.user.interfaces.request.ResetPasswordRequest;
import com.example.cae.user.interfaces.request.UpdateUserRequest;
import com.example.cae.user.interfaces.request.UpdateUserStatusRequest;
import com.example.cae.user.interfaces.request.UserPageQueryRequest;
import com.example.cae.user.interfaces.response.UserDetailResponse;
import com.example.cae.user.interfaces.response.UserListItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserFacade userFacade;

	public UserController(UserFacade userFacade) {
		this.userFacade = userFacade;
	}

	@GetMapping
	public Result<PageResult<UserListItemResponse>> pageUsers(UserPageQueryRequest request) {
		return Result.success(userFacade.pageUsers(request));
	}

	@PostMapping
	public Result<Void> createUser(@RequestBody CreateUserRequest request) {
		userFacade.createUser(request);
		return Result.success();
	}

	@GetMapping("/{id}")
	public Result<UserDetailResponse> getById(@PathVariable("id") Long id) {
		return Result.success(userFacade.getById(id));
	}

	@PutMapping("/{id}")
	public Result<Void> updateUser(@PathVariable("id") Long id, @RequestBody UpdateUserRequest request) {
		userFacade.updateUser(id, request);
		return Result.success();
	}

	@PutMapping("/{id}/status")
	public Result<Void> updateStatus(@PathVariable("id") Long id, @RequestBody UpdateUserStatusRequest request) {
		userFacade.updateStatus(id, request);
		return Result.success();
	}

	@PostMapping("/{id}/reset-password")
	public Result<Void> resetPassword(@PathVariable("id") Long id, @RequestBody ResetPasswordRequest request) {
		userFacade.resetPassword(id, request);
		return Result.success();
	}
}

