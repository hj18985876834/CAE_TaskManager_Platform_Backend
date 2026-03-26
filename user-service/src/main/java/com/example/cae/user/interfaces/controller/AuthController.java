package com.example.cae.user.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.user.application.service.AuthAppService;
import com.example.cae.user.interfaces.request.LoginRequest;
import com.example.cae.user.interfaces.response.LoginResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthAppService authAppService;

	public AuthController(AuthAppService authAppService) {
		this.authAppService = authAppService;
	}

	@PostMapping("/login")
	public Result<LoginResponse> login(@RequestBody LoginRequest request) {
		return Result.success(authAppService.login(request));
	}
}

