package com.example.cae.scheduler.infrastructure.persistence.mapper;

import com.example.cae.scheduler.infrastructure.persistence.entity.NodeSolverCapabilityPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NodeSolverCapabilityMapper {
	@Select("SELECT id, node_id AS nodeId, solver_id AS solverId, solver_version AS solverVersion, enabled, created_at AS createdAt FROM node_solver_capability WHERE node_id = #{nodeId}")
	List<NodeSolverCapabilityPO> selectByNodeId(Long nodeId);

	@Select("SELECT id, node_id AS nodeId, solver_id AS solverId, solver_version AS solverVersion, enabled, created_at AS createdAt FROM node_solver_capability WHERE solver_id = #{solverId} AND enabled = 1")
	List<NodeSolverCapabilityPO> selectBySolverId(Long solverId);

	@Delete("DELETE FROM node_solver_capability WHERE node_id = #{nodeId}")
	int deleteByNodeId(Long nodeId);

	@Insert({
		"<script>",
		"INSERT INTO node_solver_capability(node_id, solver_id, enabled) VALUES",
		"<foreach collection='solverIds' item='solverId' separator=','>",
		"(#{nodeId}, #{solverId}, 1)",
		"</foreach>",
		"</script>"
	})
	int batchInsert(@Param("nodeId") Long nodeId, @Param("solverIds") List<Long> solverIds);
}
