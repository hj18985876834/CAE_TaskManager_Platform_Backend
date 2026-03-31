package com.example.cae.user.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.user.application.facade.AuthFacade;
import com.example.cae.user.interfaces.request.LoginRequest;
import com.example.cae.user.interfaces.response.CurrentUserResponse;
import com.example.cae.user.interfaces.response.LoginResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthFacade authFacade;

	public AuthController(AuthFacade authFacade) {
		this.authFacade = authFacade;
	}

	@PostMapping("/login")
	public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return Result.success(authFacade.login(request));
	}

	@GetMapping("/me")
	public Result<CurrentUserResponse> me(@RequestHeader("X-User-Id") @Positive(message = "X-User-Id必须大于0") Long userId) {
		return Result.success(authFacade.currentUser(userId));
	}

	@PostMapping("/logout")
	public Result<Void> logout(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
		return Result.success();
	}
}
