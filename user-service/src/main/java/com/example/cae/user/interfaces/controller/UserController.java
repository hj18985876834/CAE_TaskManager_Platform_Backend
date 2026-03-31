package com.example.cae.user.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.common.response.PageResult;
import com.example.cae.user.application.facade.UserFacade;
import com.example.cae.user.interfaces.request.CreateUserRequest;
import com.example.cae.user.interfaces.request.ResetPasswordRequest;
import com.example.cae.user.interfaces.request.UpdateUserRequest;
import com.example.cae.user.interfaces.request.UpdateUserStatusRequest;
import com.example.cae.user.interfaces.request.UserPageQueryRequest;
import com.example.cae.user.interfaces.response.UserCreateResponse;
import com.example.cae.user.interfaces.response.UserDetailResponse;
import com.example.cae.user.interfaces.response.UserListItemResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserFacade userFacade;

	public UserController(UserFacade userFacade) {
		this.userFacade = userFacade;
	}

	@GetMapping
	public Result<PageResult<UserListItemResponse>> pageUsers(@Valid UserPageQueryRequest request) {
		return Result.success(userFacade.pageUsers(request));
	}

	@PostMapping
	public Result<UserCreateResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
		return Result.success(userFacade.createUser(request));
	}

	@GetMapping("/{id}")
	public Result<UserDetailResponse> getById(@PathVariable("id") @Positive(message = "id必须大于0") Long id) {
		return Result.success(userFacade.getById(id));
	}

	@PutMapping("/{id}")
	public Result<Void> updateUser(@PathVariable("id") @Positive(message = "id必须大于0") Long id, @Valid @RequestBody UpdateUserRequest request) {
		userFacade.updateUser(id, request);
		return Result.success();
	}

	@PutMapping("/{id}/status")
	public Result<Void> updateStatus(@PathVariable("id") @Positive(message = "id必须大于0") Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
		userFacade.updateStatus(id, request);
		return Result.success();
	}

	@PostMapping("/{id}/status")
	public Result<Void> updateStatusPost(@PathVariable("id") @Positive(message = "id必须大于0") Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
		userFacade.updateStatus(id, request);
		return Result.success();
	}

	@PostMapping("/{id}/reset-password")
	public Result<Void> resetPassword(@PathVariable("id") @Positive(message = "id必须大于0") Long id, @Valid @RequestBody ResetPasswordRequest request) {
		userFacade.resetPassword(id, request);
		return Result.success();
	}
}
