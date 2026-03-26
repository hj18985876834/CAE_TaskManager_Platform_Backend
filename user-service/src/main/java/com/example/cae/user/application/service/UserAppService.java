package com.example.cae.user.application.service;

import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.PageResult;
import com.example.cae.user.application.assembler.UserAssembler;
import com.example.cae.user.domain.model.Role;
import com.example.cae.user.domain.model.User;
import com.example.cae.user.domain.repository.RoleRepository;
import com.example.cae.user.domain.repository.UserRepository;
import com.example.cae.user.domain.service.PasswordDomainService;
import com.example.cae.user.domain.service.UserDomainService;
import com.example.cae.user.interfaces.request.CreateUserRequest;
import com.example.cae.user.interfaces.request.ResetPasswordRequest;
import com.example.cae.user.interfaces.request.UpdateUserRequest;
import com.example.cae.user.interfaces.request.UpdateUserStatusRequest;
import com.example.cae.user.interfaces.request.UserPageQueryRequest;
import com.example.cae.user.interfaces.response.UserDetailResponse;
import com.example.cae.user.interfaces.response.UserListItemResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserAppService {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserDomainService userDomainService;
	private final PasswordDomainService passwordDomainService;

	public UserAppService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserDomainService userDomainService,
			PasswordDomainService passwordDomainService
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userDomainService = userDomainService;
		this.passwordDomainService = passwordDomainService;
	}

	public PageResult<UserListItemResponse> pageUsers(UserPageQueryRequest request) {
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		int pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
		long offset = (long) (pageNum - 1) * pageSize;

		long total = userRepository.count(request);
		List<User> users = userRepository.page(request, offset, pageSize);
		List<UserListItemResponse> records = new ArrayList<>();
		for (User user : users) {
			Role role = roleRepository.findById(user.getRoleId()).orElse(null);
			records.add(UserAssembler.toUserListItem(user, role == null ? "USER" : role.getRoleCode()));
		}
		return PageResult.of(total, pageNum, pageSize, records);
	}

	public void createUser(CreateUserRequest request) {
		if (request == null) {
			throw new BizException(400, "request is empty");
		}
		userDomainService.checkUsernameUnique(request.getUsername());

		if (request.getRoleId() == null || roleRepository.findById(request.getRoleId()).isEmpty()) {
			throw new BizException(400, "role not found");
		}

		User user = UserAssembler.toDomain(request);
		user.setPassword(passwordDomainService.encode(request.getPassword()));
		user.enable();
		userRepository.save(user);
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

	public void updateUser(Long userId, UpdateUserRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BizException(404, "user not found"));

		if (request == null) {
			throw new BizException(400, "request is empty");
		}
		if (request.getRoleId() != null && roleRepository.findById(request.getRoleId()).isEmpty()) {
			throw new BizException(400, "role not found");
		}

		if (request.getRealName() != null) {
			user.setRealName(request.getRealName());
		}
		if (request.getRoleId() != null) {
			user.setRoleId(request.getRoleId());
		}
		userRepository.update(user);
	}

	public void updateStatus(Long userId, UpdateUserStatusRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BizException(404, "user not found"));
		if (request == null || request.getStatus() == null) {
			throw new BizException(400, "status is empty");
		}

		if (request.getStatus() == 1) {
			user.enable();
		} else {
			user.disable();
		}
		userRepository.update(user);
	}

	public void resetPassword(Long userId, ResetPasswordRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BizException(404, "user not found"));
		if (request == null || request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
			throw new BizException(400, "new password is empty");
		}
		user.resetPassword(passwordDomainService.encode(request.getNewPassword()));
		userRepository.update(user);
	}
}

