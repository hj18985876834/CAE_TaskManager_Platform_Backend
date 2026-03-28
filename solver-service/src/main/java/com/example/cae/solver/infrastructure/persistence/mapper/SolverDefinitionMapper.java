package com.example.cae.solver.infrastructure.persistence.mapper;

import com.example.cae.solver.infrastructure.persistence.entity.SolverDefinitionPO;
import com.example.cae.solver.interfaces.request.SolverPageQueryRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SolverDefinitionMapper {
	@Select("SELECT id, solver_code AS solverCode, solver_name AS solverName, version, exec_mode AS execMode, exec_path AS execPath, enabled, description AS remark, created_at AS createdAt, updated_at AS updatedAt FROM solver_definition WHERE id = #{id} LIMIT 1")
	SolverDefinitionPO selectById(Long id);

	@Select("SELECT id, solver_code AS solverCode, solver_name AS solverName, version, exec_mode AS execMode, exec_path AS execPath, enabled, description AS remark, created_at AS createdAt, updated_at AS updatedAt FROM solver_definition WHERE solver_code = #{solverCode} LIMIT 1")
	SolverDefinitionPO selectBySolverCode(String solverCode);

	@Insert("INSERT INTO solver_definition(solver_code, solver_name, version, exec_mode, exec_path, enabled, description) VALUES(#{solverCode}, #{solverName}, #{version}, #{execMode}, #{execPath}, #{enabled}, #{remark})")
	int insert(SolverDefinitionPO po);

	@Update("UPDATE solver_definition SET solver_name = #{solverName}, version = #{version}, exec_mode = #{execMode}, exec_path = #{execPath}, enabled = #{enabled}, description = #{remark} WHERE id = #{id}")
	int updateById(SolverDefinitionPO po);

	@Select({
		"<script>",
		"SELECT id, solver_code AS solverCode, solver_name AS solverName, version, exec_mode AS execMode, exec_path AS execPath, enabled, description AS remark, created_at AS createdAt, updated_at AS updatedAt FROM solver_definition",
		"<where>",
		"  <if test='request.solverCode != null and request.solverCode != \"\"'>AND solver_code LIKE CONCAT('%', #{request.solverCode}, '%')</if>",
		"  <if test='request.solverName != null and request.solverName != \"\"'>AND solver_name LIKE CONCAT('%', #{request.solverName}, '%')</if>",
		"  <if test='request.enabled != null'>AND enabled = #{request.enabled}</if>",
		"</where>",
		"ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
		"</script>"
	})
	List<SolverDefinitionPO> selectPage(@Param("request") SolverPageQueryRequest request, @Param("offset") long offset, @Param("pageSize") long pageSize);

	@Select({
		"<script>",
		"SELECT COUNT(1) FROM solver_definition",
		"<where>",
		"  <if test='request.solverCode != null and request.solverCode != \"\"'>AND solver_code LIKE CONCAT('%', #{request.solverCode}, '%')</if>",
		"  <if test='request.solverName != null and request.solverName != \"\"'>AND solver_name LIKE CONCAT('%', #{request.solverName}, '%')</if>",
		"  <if test='request.enabled != null'>AND enabled = #{request.enabled}</if>",
		"</where>",
		"</script>"
	})
	long count(@Param("request") SolverPageQueryRequest request);
}
