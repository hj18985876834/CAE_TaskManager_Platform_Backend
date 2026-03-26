package com.example.cae.user.infrastructure.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class PasswordEncoderService {
	public String encode(String rawPassword) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			for (byte item : hashed) {
				builder.append(String.format("%02x", item));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not supported", ex);
		}
	}

	public boolean matches(String rawPassword, String encodedPassword) {
		if (encodedPassword == null) {
			return false;
		}
		return encode(rawPassword).equals(encodedPassword) || rawPassword.equals(encodedPassword);
	}
}

