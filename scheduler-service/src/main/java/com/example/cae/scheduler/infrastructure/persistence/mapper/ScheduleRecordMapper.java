package com.example.cae.scheduler.infrastructure.persistence.mapper;

import com.example.cae.scheduler.infrastructure.persistence.entity.ScheduleRecordPO;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScheduleRecordMapper {
	@Insert("INSERT INTO schedule_record(task_id, node_id, strategy_name, schedule_status, schedule_message) VALUES(#{taskId}, #{nodeId}, #{strategyName}, #{scheduleStatus}, #{scheduleMessage})")
	int insert(ScheduleRecordPO po);

	@Select({
		"<script>",
		"SELECT id, task_id AS taskId, node_id AS nodeId, strategy_name AS strategyName, schedule_status AS scheduleStatus, schedule_message AS scheduleMessage, created_at AS createdAt FROM schedule_record",
		"<where>",
		"  <if test='request.taskId != null'>AND task_id = #{request.taskId}</if>",
		"  <if test='request.nodeId != null'>AND node_id = #{request.nodeId}</if>",
		"  <if test='request.scheduleStatus != null and request.scheduleStatus != \"\"'>AND schedule_status = #{request.scheduleStatus}</if>",
		"  <if test='request.strategyName != null and request.strategyName != \"\"'>AND strategy_name = #{request.strategyName}</if>",
		"</where>",
		"ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
		"</script>"
	})
	List<ScheduleRecordPO> selectPage(@Param("request") SchedulePageQueryRequest request, @Param("offset") long offset, @Param("pageSize") int pageSize);

	@Select({
		"<script>",
		"SELECT COUNT(1) FROM schedule_record",
		"<where>",
		"  <if test='request.taskId != null'>AND task_id = #{request.taskId}</if>",
		"  <if test='request.nodeId != null'>AND node_id = #{request.nodeId}</if>",
		"  <if test='request.scheduleStatus != null and request.scheduleStatus != \"\"'>AND schedule_status = #{request.scheduleStatus}</if>",
		"  <if test='request.strategyName != null and request.strategyName != \"\"'>AND strategy_name = #{request.strategyName}</if>",
		"</where>",
		"</script>"
	})
	long countPage(@Param("request") SchedulePageQueryRequest request);
}
