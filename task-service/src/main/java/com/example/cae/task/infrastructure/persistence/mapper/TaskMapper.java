package com.example.cae.task.infrastructure.persistence.mapper;

import com.example.cae.task.infrastructure.persistence.entity.TaskPO;
import com.example.cae.task.interfaces.request.TaskListQueryRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TaskMapper {
	@Select("SELECT id, task_no AS taskNo, task_name AS taskName, user_id AS userId, solver_id AS solverId, profile_id AS profileId, task_type AS taskType, status, priority, node_id AS nodeId, params_json AS paramsJson, submit_time AS submitTime, start_time AS startTime, end_time AS endTime, fail_type AS failType, fail_message AS failMessage, deleted_flag AS deletedFlag, created_at AS createdAt, updated_at AS updatedAt FROM sim_task WHERE id = #{id} AND deleted_flag = 0 LIMIT 1")
	TaskPO selectById(Long id);

	@Options(useGeneratedKeys = true, keyProperty = "id")
	@Insert("INSERT INTO sim_task(task_no, task_name, user_id, solver_id, profile_id, task_type, status, priority, node_id, params_json, submit_time, start_time, end_time, fail_type, fail_message, deleted_flag) VALUES(#{taskNo}, #{taskName}, #{userId}, #{solverId}, #{profileId}, #{taskType}, #{status}, #{priority}, #{nodeId}, #{paramsJson}, #{submitTime}, #{startTime}, #{endTime}, #{failType}, #{failMessage}, #{deletedFlag})")
	int insert(TaskPO po);

	@Update("UPDATE sim_task SET task_name = #{taskName}, status = #{status}, priority = #{priority}, node_id = #{nodeId}, params_json = #{paramsJson}, submit_time = #{submitTime}, start_time = #{startTime}, end_time = #{endTime}, fail_type = #{failType}, fail_message = #{failMessage} WHERE id = #{id}")
	int updateById(TaskPO po);

	@Select({
		"<script>",
		"SELECT id, task_no AS taskNo, task_name AS taskName, user_id AS userId, solver_id AS solverId, profile_id AS profileId, task_type AS taskType, status, priority, node_id AS nodeId, params_json AS paramsJson, submit_time AS submitTime, start_time AS startTime, end_time AS endTime, fail_type AS failType, fail_message AS failMessage, deleted_flag AS deletedFlag, created_at AS createdAt, updated_at AS updatedAt FROM sim_task",
		"<where>",
		"deleted_flag = 0",
		"AND user_id = #{userId}",
		"  <if test='request.taskName != null and request.taskName != \"\"'>AND task_name LIKE CONCAT('%', #{request.taskName}, '%')</if>",
		"  <if test='request.taskNo != null and request.taskNo != \"\"'>AND task_no = #{request.taskNo}</if>",
		"  <if test='request.status != null and request.status != \"\"'>AND status = #{request.status}</if>",
		"  <if test='request.solverId != null'>AND solver_id = #{request.solverId}</if>",
		"  <if test='request.profileId != null'>AND profile_id = #{request.profileId}</if>",
		"  <if test='request.nodeId != null'>AND node_id = #{request.nodeId}</if>",
		"</where>",
		"ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
		"</script>"
	})
	List<TaskPO> selectMyPage(@Param("request") TaskListQueryRequest request, @Param("userId") Long userId, @Param("offset") long offset, @Param("pageSize") long pageSize);

	@Select({
		"<script>",
		"SELECT COUNT(1) FROM sim_task",
		"<where>",
		"deleted_flag = 0",
		"AND user_id = #{userId}",
		"  <if test='request.taskName != null and request.taskName != \"\"'>AND task_name LIKE CONCAT('%', #{request.taskName}, '%')</if>",
		"  <if test='request.taskNo != null and request.taskNo != \"\"'>AND task_no = #{request.taskNo}</if>",
		"  <if test='request.status != null and request.status != \"\"'>AND status = #{request.status}</if>",
		"  <if test='request.solverId != null'>AND solver_id = #{request.solverId}</if>",
		"  <if test='request.profileId != null'>AND profile_id = #{request.profileId}</if>",
		"  <if test='request.nodeId != null'>AND node_id = #{request.nodeId}</if>",
		"</where>",
		"</script>"
	})
	long countMyPage(@Param("request") TaskListQueryRequest request, @Param("userId") Long userId);

	@Select({
		"<script>",
		"SELECT id, task_no AS taskNo, task_name AS taskName, user_id AS userId, solver_id AS solverId, profile_id AS profileId, task_type AS taskType, status, priority, node_id AS nodeId, params_json AS paramsJson, submit_time AS submitTime, start_time AS startTime, end_time AS endTime, fail_type AS failType, fail_message AS failMessage, deleted_flag AS deletedFlag, created_at AS createdAt, updated_at AS updatedAt FROM sim_task",
		"<where>",
		"deleted_flag = 0",
		"  <if test='request.userId != null'>AND user_id = #{request.userId}</if>",
		"  <if test='request.taskName != null and request.taskName != \"\"'>AND task_name LIKE CONCAT('%', #{request.taskName}, '%')</if>",
		"  <if test='request.taskNo != null and request.taskNo != \"\"'>AND task_no = #{request.taskNo}</if>",
		"  <if test='request.status != null and request.status != \"\"'>AND status = #{request.status}</if>",
		"  <if test='request.solverId != null'>AND solver_id = #{request.solverId}</if>",
		"  <if test='request.profileId != null'>AND profile_id = #{request.profileId}</if>",
		"  <if test='request.nodeId != null'>AND node_id = #{request.nodeId}</if>",
		"</where>",
		"ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
		"</script>"
	})
	List<TaskPO> selectAdminPage(@Param("request") TaskListQueryRequest request, @Param("offset") long offset, @Param("pageSize") long pageSize);

	@Select({
		"<script>",
		"SELECT COUNT(1) FROM sim_task",
		"<where>",
		"deleted_flag = 0",
		"  <if test='request.userId != null'>AND user_id = #{request.userId}</if>",
		"  <if test='request.taskName != null and request.taskName != \"\"'>AND task_name LIKE CONCAT('%', #{request.taskName}, '%')</if>",
		"  <if test='request.taskNo != null and request.taskNo != \"\"'>AND task_no = #{request.taskNo}</if>",
		"  <if test='request.status != null and request.status != \"\"'>AND status = #{request.status}</if>",
		"  <if test='request.solverId != null'>AND solver_id = #{request.solverId}</if>",
		"  <if test='request.profileId != null'>AND profile_id = #{request.profileId}</if>",
		"  <if test='request.nodeId != null'>AND node_id = #{request.nodeId}</if>",
		"</where>",
		"</script>"
	})
	long countAdminPage(@Param("request") TaskListQueryRequest request);

	@Select("SELECT id, task_no AS taskNo, task_name AS taskName, user_id AS userId, solver_id AS solverId, profile_id AS profileId, task_type AS taskType, status, priority, node_id AS nodeId, params_json AS paramsJson, submit_time AS submitTime, start_time AS startTime, end_time AS endTime, fail_type AS failType, fail_message AS failMessage, deleted_flag AS deletedFlag, created_at AS createdAt, updated_at AS updatedAt FROM sim_task WHERE status = #{status} AND deleted_flag = 0 ORDER BY id ASC")
	List<TaskPO> selectByStatus(String status);
}

