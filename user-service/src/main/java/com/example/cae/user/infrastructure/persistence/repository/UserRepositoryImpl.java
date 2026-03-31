package com.example.cae.user.infrastructure.persistence.repository;

import com.example.cae.user.domain.model.User;
import com.example.cae.user.domain.repository.UserRepository;
import com.example.cae.user.infrastructure.persistence.entity.UserPO;
import com.example.cae.user.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import com.example.cae.user.interfaces.request.UserPageQueryRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

	@Override
	public void save(User user) {
		UserPO po = toPO(user);
		userMapper.insert(po);
		user.setId(po.getId());
	}

	@Override
	public void update(User user) {
		userMapper.updateById(toPO(user));
	}

	@Override
	public List<User> page(UserPageQueryRequest request, long offset, long pageSize) {
		return userMapper.selectPage(request, offset, pageSize).stream().map(this::toDomain).collect(Collectors.toList());
	}

	@Override
	public long count(UserPageQueryRequest request) {
		return userMapper.count(request);
	}

	private User toDomain(UserPO po) {
		User user = new User();
		user.setId(po.getId());
		user.setUsername(po.getUsername());
		user.setPassword(po.getPassword());
		user.setRealName(po.getRealName());
		user.setRoleId(po.getRoleId());
		user.setStatus(po.getStatus());
		user.setCreatedAt(po.getCreatedAt());
		user.setUpdatedAt(po.getUpdatedAt());
		return user;
	}

	private UserPO toPO(User user) {
		UserPO po = new UserPO();
		po.setId(user.getId());
		po.setUsername(user.getUsername());
		po.setPassword(user.getPassword());
		po.setRealName(user.getRealName());
		po.setRoleId(user.getRoleId());
		po.setStatus(user.getStatus());
		return po;
	}
}
