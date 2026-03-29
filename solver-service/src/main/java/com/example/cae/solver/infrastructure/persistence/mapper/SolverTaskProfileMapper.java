package com.example.cae.solver.infrastructure.persistence.mapper;

import com.example.cae.solver.infrastructure.persistence.entity.SolverTaskProfilePO;
import com.example.cae.solver.interfaces.request.ProfilePageQueryRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SolverTaskProfileMapper {
	@Select("SELECT id, solver_id AS solverId, profile_code AS profileCode, task_type AS taskType, profile_name AS profileName, command_template AS commandTemplate, params_schema_json AS paramsSchemaJson, parser_name AS parserName, timeout_seconds AS timeoutSeconds, enabled, description, created_at AS createdAt, updated_at AS updatedAt FROM solver_task_profile WHERE id = #{id} LIMIT 1")
	SolverTaskProfilePO selectById(Long id);

	@Select("SELECT id, solver_id AS solverId, profile_code AS profileCode, task_type AS taskType, profile_name AS profileName, command_template AS commandTemplate, params_schema_json AS paramsSchemaJson, parser_name AS parserName, timeout_seconds AS timeoutSeconds, enabled, description, created_at AS createdAt, updated_at AS updatedAt FROM solver_task_profile WHERE solver_id = #{solverId} AND profile_code = #{profileCode} LIMIT 1")
	SolverTaskProfilePO selectBySolverIdAndProfileCode(@Param("solverId") Long solverId, @Param("profileCode") String profileCode);

	@Insert("INSERT INTO solver_task_profile(solver_id, profile_code, task_type, profile_name, command_template, params_schema_json, parser_name, timeout_seconds, enabled, description) VALUES(#{solverId}, #{profileCode}, #{taskType}, #{profileName}, #{commandTemplate}, #{paramsSchemaJson}, #{parserName}, #{timeoutSeconds}, #{enabled}, #{description})")
	int insert(SolverTaskProfilePO po);

	@Update("UPDATE solver_task_profile SET task_type = #{taskType}, profile_name = #{profileName}, command_template = #{commandTemplate}, params_schema_json = #{paramsSchemaJson}, parser_name = #{parserName}, timeout_seconds = #{timeoutSeconds}, enabled = #{enabled}, description = #{description} WHERE id = #{id}")
	int updateById(SolverTaskProfilePO po);

	@Select({
		"<script>",
		"SELECT id, solver_id AS solverId, profile_code AS profileCode, task_type AS taskType, profile_name AS profileName, command_template AS commandTemplate, params_schema_json AS paramsSchemaJson, parser_name AS parserName, timeout_seconds AS timeoutSeconds, enabled, description, created_at AS createdAt, updated_at AS updatedAt FROM solver_task_profile",
		"<where>",
		"  <if test='request.solverId != null'>AND solver_id = #{request.solverId}</if>",
		"  <if test='request.taskType != null and request.taskType != \"\"'>AND task_type = #{request.taskType}</if>",
		"  <if test='request.profileCode != null and request.profileCode != \"\"'>AND profile_code LIKE CONCAT('%', #{request.profileCode}, '%')</if>",
		"  <if test='request.enabled != null'>AND enabled = #{request.enabled}</if>",
		"</where>",
		"ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
		"</script>"
	})
	List<SolverTaskProfilePO> selectPage(@Param("request") ProfilePageQueryRequest request, @Param("offset") long offset, @Param("pageSize") long pageSize);

	@Select({
		"<script>",
		"SELECT COUNT(1) FROM solver_task_profile",
		"<where>",
		"  <if test='request.solverId != null'>AND solver_id = #{request.solverId}</if>",
		"  <if test='request.taskType != null and request.taskType != \"\"'>AND task_type = #{request.taskType}</if>",
		"  <if test='request.profileCode != null and request.profileCode != \"\"'>AND profile_code LIKE CONCAT('%', #{request.profileCode}, '%')</if>",
		"  <if test='request.enabled != null'>AND enabled = #{request.enabled}</if>",
		"</where>",
		"</script>"
	})
	long count(@Param("request") ProfilePageQueryRequest request);

	@Select("SELECT id, solver_id AS solverId, profile_code AS profileCode, task_type AS taskType, profile_name AS profileName, command_template AS commandTemplate, params_schema_json AS paramsSchemaJson, parser_name AS parserName, timeout_seconds AS timeoutSeconds, enabled, description, created_at AS createdAt, updated_at AS updatedAt FROM solver_task_profile WHERE solver_id = #{solverId} AND enabled = 1 ORDER BY id DESC")
	List<SolverTaskProfilePO> selectEnabledBySolverId(Long solverId);

	@Select("SELECT id, solver_id AS solverId, profile_code AS profileCode, task_type AS taskType, profile_name AS profileName, command_template AS commandTemplate, params_schema_json AS paramsSchemaJson, parser_name AS parserName, timeout_seconds AS timeoutSeconds, enabled, description, created_at AS createdAt, updated_at AS updatedAt FROM solver_task_profile WHERE solver_id = #{solverId} ORDER BY id DESC")
	List<SolverTaskProfilePO> selectBySolverId(Long solverId);
}
