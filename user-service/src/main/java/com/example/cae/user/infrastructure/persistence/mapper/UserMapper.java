package com.example.cae.user.infrastructure.persistence.mapper;

import com.example.cae.user.infrastructure.persistence.entity.UserPO;
import com.example.cae.user.interfaces.request.UserPageQueryRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {
	@Select("SELECT id, username, password, real_name AS realName, role_id AS roleId, status, created_at AS createdAt, updated_at AS updatedAt FROM sys_user WHERE username = #{username} LIMIT 1")
	UserPO selectByUsername(String username);

	@Select("SELECT id, username, password, real_name AS realName, role_id AS roleId, status, created_at AS createdAt, updated_at AS updatedAt FROM sys_user WHERE id = #{id} LIMIT 1")
	UserPO selectById(Long id);

	@Insert("INSERT INTO sys_user(username, password, real_name, role_id, status) VALUES(#{username}, #{password}, #{realName}, #{roleId}, #{status})")
	int insert(UserPO po);

	@Update("UPDATE sys_user SET real_name = #{realName}, role_id = #{roleId}, status = #{status}, password = #{password} WHERE id = #{id}")
	int updateById(UserPO po);

	@Select({
		"<script>",
		"SELECT id, username, password, real_name AS realName, role_id AS roleId, status, created_at AS createdAt, updated_at AS updatedAt FROM sys_user",
		"<where>",
		"  <if test='request.username != null and request.username != \"\"'>AND username LIKE CONCAT('%', #{request.username}, '%')</if>",
		"  <if test='request.realName != null and request.realName != \"\"'>AND real_name LIKE CONCAT('%', #{request.realName}, '%')</if>",
		"  <if test='request.status != null'>AND status = #{request.status}</if>",
		"  <if test='request.roleId != null'>AND role_id = #{request.roleId}</if>",
		"</where>",
		"ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
		"</script>"
	})
	List<UserPO> selectPage(@Param("request") UserPageQueryRequest request, @Param("offset") long offset, @Param("pageSize") long pageSize);

	@Select({
		"<script>",
		"SELECT COUNT(1) FROM sys_user",
		"<where>",
		"  <if test='request.username != null and request.username != \"\"'>AND username LIKE CONCAT('%', #{request.username}, '%')</if>",
		"  <if test='request.realName != null and request.realName != \"\"'>AND real_name LIKE CONCAT('%', #{request.realName}, '%')</if>",
		"  <if test='request.status != null'>AND status = #{request.status}</if>",
		"  <if test='request.roleId != null'>AND role_id = #{request.roleId}</if>",
		"</where>",
		"</script>"
	})
	long count(@Param("request") UserPageQueryRequest request);
}

