package com.example.cae.user.infrastructure.persistence.repository;

import com.example.cae.user.domain.model.Role;
import com.example.cae.user.domain.repository.RoleRepository;
import com.example.cae.user.infrastructure.persistence.entity.RolePO;
import com.example.cae.user.infrastructure.persistence.mapper.RoleMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RoleRepositoryImpl implements RoleRepository {
	private final RoleMapper roleMapper;

	public RoleRepositoryImpl(RoleMapper roleMapper) {
		this.roleMapper = roleMapper;
	}

	@Override
	public Optional<Role> findById(Long id) {
		return Optional.ofNullable(roleMapper.selectById(id)).map(this::toDomain);
	}

	private Role toDomain(RolePO po) {
		Role role = new Role();
		role.setId(po.getId());
		role.setRoleCode(po.getRoleCode());
		role.setRoleName(po.getRoleName());
		return role;
	}
}

