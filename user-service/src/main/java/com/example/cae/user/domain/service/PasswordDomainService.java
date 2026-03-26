package com.example.cae.user.domain.service;

import com.example.cae.user.infrastructure.security.PasswordEncoderService;
import org.springframework.stereotype.Service;

@Service
public class PasswordDomainService {
	private final PasswordEncoderService passwordEncoderService;

	public PasswordDomainService(PasswordEncoderService passwordEncoderService) {
		this.passwordEncoderService = passwordEncoderService;
	}

	public String encode(String rawPassword) {
		return passwordEncoderService.encode(rawPassword);
	}

	public boolean matches(String rawPassword, String encodedPassword) {
		return passwordEncoderService.matches(rawPassword, encodedPassword);
	}
}

