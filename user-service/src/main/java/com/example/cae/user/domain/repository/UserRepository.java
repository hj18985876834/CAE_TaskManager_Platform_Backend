package com.example.cae.user.domain.repository;

import com.example.cae.user.domain.model.User;
import com.example.cae.user.interfaces.request.UserPageQueryRequest;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
	Optional<User> findById(Long id);

	Optional<User> findByUsername(String username);

	void save(User user);

	void update(User user);

	List<User> page(UserPageQueryRequest request, long offset, long pageSize);

	long count(UserPageQueryRequest request);
}

