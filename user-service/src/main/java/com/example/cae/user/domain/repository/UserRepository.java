package com.example.cae.user.domain.repository;

import com.example.cae.user.domain.model.User;

import java.util.Optional;

public interface UserRepository {
	Optional<User> findById(Long id);

	Optional<User> findByUsername(String username);
}

