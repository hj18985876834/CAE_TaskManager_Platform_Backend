package com.example.cae.user.infrastructure.persistence.mapper;

import com.example.cae.user.infrastructure.persistence.entity.RolePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper {
	@Select("SELECT id, role_code AS roleCode, role_name AS roleName FROM sys_role WHERE id = #{id} LIMIT 1")
	RolePO selectById(Long id);
}

