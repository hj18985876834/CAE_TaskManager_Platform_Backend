package com.example.cae.user.infrastructure.persistence.mapper;

import com.example.cae.user.infrastructure.persistence.entity.UserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
	@Select("SELECT id, username, password, real_name AS realName, role_id AS roleId, status FROM sys_user WHERE username = #{username} LIMIT 1")
	UserPO selectByUsername(String username);

	@Select("SELECT id, username, password, real_name AS realName, role_id AS roleId, status FROM sys_user WHERE id = #{id} LIMIT 1")
	UserPO selectById(Long id);
}

