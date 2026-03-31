package com.example.cae.user.domain.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserDomainService {
	private final UserRepository userRepository;

	public UserDomainService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public void checkUsernameUnique(String username) {
		if (username == null || username.trim().isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "username is empty");
		}
		if (userRepository.findByUsername(username).isPresent()) {
			throw new BizException(ErrorCodeConstants.USERNAME_ALREADY_EXISTS, "username already exists");
		}
	}
}
