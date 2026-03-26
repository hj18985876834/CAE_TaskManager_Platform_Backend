package com.example.cae.task.infrastructure.persistence.mapper;

import com.example.cae.task.infrastructure.persistence.entity.TaskStatusHistoryPO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskStatusHistoryMapper {
	@Insert("INSERT INTO task_status_history(task_id, from_status, to_status, change_reason, operator_type, operator_id) VALUES(#{taskId}, #{fromStatus}, #{toStatus}, #{changeReason}, #{operatorType}, #{operatorId})")
	int insert(TaskStatusHistoryPO po);

	@Select("SELECT id, task_id AS taskId, from_status AS fromStatus, to_status AS toStatus, change_reason AS changeReason, operator_type AS operatorType, operator_id AS operatorId, created_at AS createdAt FROM task_status_history WHERE task_id = #{taskId} ORDER BY id ASC")
	List<TaskStatusHistoryPO> selectByTaskId(Long taskId);
}

