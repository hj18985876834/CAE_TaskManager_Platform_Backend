package com.example.cae.user.application.facade;

import com.example.cae.user.application.service.AuthAppService;
import com.example.cae.user.interfaces.request.LoginRequest;
import com.example.cae.user.interfaces.response.CurrentUserResponse;
import com.example.cae.user.interfaces.response.LoginResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthFacade {
	private final AuthAppService authAppService;

	public AuthFacade(AuthAppService authAppService) {
		this.authAppService = authAppService;
	}

	public LoginResponse login(LoginRequest request) {
		return authAppService.login(request);
	}

	public CurrentUserResponse currentUser(Long userId) {
		return authAppService.getCurrentUser(userId);
	}
}

