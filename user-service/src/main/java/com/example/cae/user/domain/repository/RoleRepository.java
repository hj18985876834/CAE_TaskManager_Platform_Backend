package com.example.cae.user.domain.repository;

import com.example.cae.user.domain.model.Role;

import java.util.Optional;

public interface RoleRepository {
	Optional<Role> findById(Long id);
}

