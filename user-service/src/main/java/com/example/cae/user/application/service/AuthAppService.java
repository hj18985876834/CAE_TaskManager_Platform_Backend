package com.example.cae.user.application.service;

import com.example.cae.common.exception.BizException;
import com.example.cae.user.application.assembler.UserAssembler;
import com.example.cae.user.domain.model.Role;
import com.example.cae.user.domain.model.User;
import com.example.cae.user.domain.repository.RoleRepository;
import com.example.cae.user.domain.repository.UserRepository;
import com.example.cae.user.infrastructure.security.JwtTokenService;
import com.example.cae.user.infrastructure.security.PasswordEncoderService;
import com.example.cae.user.interfaces.request.LoginRequest;
import com.example.cae.user.interfaces.response.CurrentUserResponse;
import com.example.cae.user.interfaces.response.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthAppService {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoderService passwordEncoderService;
	private final JwtTokenService jwtTokenService;

	public AuthAppService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoderService passwordEncoderService,
			JwtTokenService jwtTokenService
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoderService = passwordEncoderService;
		this.jwtTokenService = jwtTokenService;
	}

	public LoginResponse login(LoginRequest request) {
		if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
			throw new BizException(400, "username or password is empty");
		}

		User user = userRepository.findByUsername(request.getUsername())
				.orElseThrow(() -> new BizException(401, "username or password is invalid"));

		if (user.getStatus() == null || user.getStatus() != 1) {
			throw new BizException(403, "user is disabled");
		}

		if (!passwordEncoderService.matches(request.getPassword(), user.getPassword())) {
			throw new BizException(401, "username or password is invalid");
		}

		Role role = roleRepository.findById(user.getRoleId())
				.orElseGet(() -> {
					Role fallback = new Role();
					fallback.setId(user.getRoleId());
					fallback.setRoleCode("USER");
					fallback.setRoleName("Default User");
					return fallback;
				});

		String token = jwtTokenService.generateToken(user.getId(), role.getRoleCode());
		return UserAssembler.toLoginResponse(user, role, token);
	}

	public CurrentUserResponse getCurrentUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BizException(404, "user not found"));

		Role role = roleRepository.findById(user.getRoleId())
				.orElseGet(() -> {
					Role fallback = new Role();
					fallback.setId(user.getRoleId());
					fallback.setRoleCode("USER");
					fallback.setRoleName("Default User");
					return fallback;
				});

		return UserAssembler.toCurrentUserResponse(user, role);
	}

	public void logout(Long userId) {
		// Stateless token flow: server-side logout is currently a no-op.
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}

