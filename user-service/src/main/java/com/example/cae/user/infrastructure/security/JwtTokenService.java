package com.example.cae.user.infrastructure.security;

import com.example.cae.common.utils.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
	public String generateToken(Long userId, String roleCode) {
		return JwtUtil.generateToken(userId, roleCode);
	}
}

