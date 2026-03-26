package com.example.cae.user.application.service;

import com.example.cae.common.exception.BizException;
import com.example.cae.user.application.assembler.UserAssembler;
import com.example.cae.user.domain.model.Role;
import com.example.cae.user.domain.model.User;
import com.example.cae.user.domain.repository.RoleRepository;
import com.example.cae.user.domain.repository.UserRepository;
import com.example.cae.user.interfaces.response.UserDetailResponse;
import org.springframework.stereotype.Service;

@Service
public class UserAppService {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	public UserAppService(UserRepository userRepository, RoleRepository roleRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
	}

	public UserDetailResponse getById(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new BizException(404, "user not found"));

		Role role = roleRepository.findById(user.getRoleId())
				.orElseGet(() -> {
					Role fallback = new Role();
					fallback.setId(user.getRoleId());
					fallback.setRoleCode("USER");
					fallback.setRoleName("Default User");
					return fallback;
				});

		return UserAssembler.toUserDetailResponse(user, role);
	}
}

