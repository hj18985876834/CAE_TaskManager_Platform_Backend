package com.example.cae.user.infrastructure.persistence.repository;

import com.example.cae.user.domain.model.User;
import com.example.cae.user.domain.repository.UserRepository;
import com.example.cae.user.infrastructure.persistence.entity.UserPO;
import com.example.cae.user.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {
	private final UserMapper userMapper;

	public UserRepositoryImpl(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	@Override
	public Optional<User> findById(Long id) {
		return Optional.ofNullable(userMapper.selectById(id)).map(this::toDomain);
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return Optional.ofNullable(userMapper.selectByUsername(username)).map(this::toDomain);
	}

	private User toDomain(UserPO po) {
		User user = new User();
		user.setId(po.getId());
		user.setUsername(po.getUsername());
		user.setPassword(po.getPassword());
		user.setRealName(po.getRealName());
		user.setRoleId(po.getRoleId());
		user.setStatus(po.getStatus());
		return user;
	}
}

