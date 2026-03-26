package com.example.cae.user.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.user.application.service.UserAppService;
import com.example.cae.user.interfaces.response.UserDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserAppService userAppService;

	public UserController(UserAppService userAppService) {
		this.userAppService = userAppService;
	}

	@GetMapping("/{id}")
	public Result<UserDetailResponse> getById(@PathVariable("id") Long id) {
		return Result.success(userAppService.getById(id));
	}
}

