package com.example.cae.scheduler.infrastructure.persistence.mapper;

import com.example.cae.scheduler.infrastructure.persistence.entity.ComputeNodePO;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ComputeNodeMapper {
	@Select("SELECT id, node_code AS nodeCode, node_name AS nodeName, host, port, node_token AS nodeToken, status, enabled, max_concurrency AS maxConcurrency, running_count AS runningCount, reserved_count AS reservedCount, cpu_usage AS cpuUsage, memory_usage AS memoryUsage, last_heartbeat_time AS lastHeartbeatTime, created_at AS createdAt, updated_at AS updatedAt FROM compute_node WHERE id = #{id} LIMIT 1")
	ComputeNodePO selectById(Long id);

	@Select("SELECT id, node_code AS nodeCode, node_name AS nodeName, host, port, node_token AS nodeToken, status, enabled, max_concurrency AS maxConcurrency, running_count AS runningCount, reserved_count AS reservedCount, cpu_usage AS cpuUsage, memory_usage AS memoryUsage, last_heartbeat_time AS lastHeartbeatTime, created_at AS createdAt, updated_at AS updatedAt FROM compute_node WHERE id = #{id} LIMIT 1 FOR UPDATE")
	ComputeNodePO selectByIdForUpdate(Long id);

	@Select("SELECT id, node_code AS nodeCode, node_name AS nodeName, host, port, node_token AS nodeToken, status, enabled, max_concurrency AS maxConcurrency, running_count AS runningCount, reserved_count AS reservedCount, cpu_usage AS cpuUsage, memory_usage AS memoryUsage, last_heartbeat_time AS lastHeartbeatTime, created_at AS createdAt, updated_at AS updatedAt FROM compute_node WHERE node_code = #{nodeCode} LIMIT 1")
	ComputeNodePO selectByNodeCode(String nodeCode);

	@Select("SELECT id, node_code AS nodeCode, node_name AS nodeName, host, port, node_token AS nodeToken, status, enabled, max_concurrency AS maxConcurrency, running_count AS runningCount, reserved_count AS reservedCount, cpu_usage AS cpuUsage, memory_usage AS memoryUsage, last_heartbeat_time AS lastHeartbeatTime, created_at AS createdAt, updated_at AS updatedAt FROM compute_node WHERE node_token = #{nodeToken} LIMIT 1")
	ComputeNodePO selectByNodeToken(String nodeToken);

	@Options(useGeneratedKeys = true, keyProperty = "id")
	@Insert("INSERT INTO compute_node(node_code, node_name, host, port, node_token, status, enabled, max_concurrency, running_count, reserved_count, cpu_usage, memory_usage, last_heartbeat_time) VALUES(#{nodeCode}, #{nodeName}, #{host}, #{port}, #{nodeToken}, #{status}, #{enabled}, #{maxConcurrency}, #{runningCount}, #{reservedCount}, #{cpuUsage}, #{memoryUsage}, #{lastHeartbeatTime})")
	int insert(ComputeNodePO po);

	@Update("UPDATE compute_node SET node_name = #{nodeName}, host = #{host}, port = #{port}, node_token = #{nodeToken}, status = #{status}, enabled = #{enabled}, max_concurrency = #{maxConcurrency}, running_count = #{runningCount}, reserved_count = #{reservedCount}, cpu_usage = #{cpuUsage}, memory_usage = #{memoryUsage}, last_heartbeat_time = #{lastHeartbeatTime} WHERE id = #{id}")
	int updateById(ComputeNodePO po);

	@Select({
		"<script>",
		"SELECT id, node_code AS nodeCode, node_name AS nodeName, host, port, node_token AS nodeToken, status, enabled, max_concurrency AS maxConcurrency, running_count AS runningCount, reserved_count AS reservedCount, cpu_usage AS cpuUsage, memory_usage AS memoryUsage, last_heartbeat_time AS lastHeartbeatTime, created_at AS createdAt, updated_at AS updatedAt FROM compute_node",
		"<where>",
		"  <if test='request.nodeName != null and request.nodeName != \"\"'>AND node_name LIKE CONCAT('%', #{request.nodeName}, '%')</if>",
		"  <if test='request.status != null and request.status != \"\"'>AND status = #{request.status}</if>",
		"  <if test='request.enabled != null'>AND enabled = #{request.enabled}</if>",
		"  <if test='request.solverIdAsLong != null'>AND EXISTS (SELECT 1 FROM node_solver_capability nsc WHERE nsc.node_id = compute_node.id AND nsc.solver_id = #{request.solverIdAsLong} AND nsc.enabled = 1)</if>",
		"</where>",
		"ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
		"</script>"
	})
	List<ComputeNodePO> selectPage(@Param("request") NodePageQueryRequest request, @Param("offset") long offset, @Param("pageSize") int pageSize);

	@Select({
		"<script>",
		"SELECT COUNT(1) FROM compute_node",
		"<where>",
		"  <if test='request.nodeName != null and request.nodeName != \"\"'>AND node_name LIKE CONCAT('%', #{request.nodeName}, '%')</if>",
		"  <if test='request.status != null and request.status != \"\"'>AND status = #{request.status}</if>",
		"  <if test='request.enabled != null'>AND enabled = #{request.enabled}</if>",
		"  <if test='request.solverIdAsLong != null'>AND EXISTS (SELECT 1 FROM node_solver_capability nsc WHERE nsc.node_id = compute_node.id AND nsc.solver_id = #{request.solverIdAsLong} AND nsc.enabled = 1)</if>",
		"</where>",
		"</script>"
	})
	long countPage(@Param("request") NodePageQueryRequest request);

	@Select("SELECT id, node_code AS nodeCode, node_name AS nodeName, host, port, node_token AS nodeToken, status, enabled, max_concurrency AS maxConcurrency, running_count AS runningCount, reserved_count AS reservedCount, cpu_usage AS cpuUsage, memory_usage AS memoryUsage, last_heartbeat_time AS lastHeartbeatTime, created_at AS createdAt, updated_at AS updatedAt FROM compute_node WHERE status = #{status} ORDER BY id ASC")
	List<ComputeNodePO> selectByStatus(String status);
}
