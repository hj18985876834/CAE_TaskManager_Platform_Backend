package com.example.cae.task.infrastructure.persistence.mapper;

import com.example.cae.task.infrastructure.persistence.entity.TaskResultSummaryPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskResultSummaryMapper {
	@Select("SELECT id, task_id AS taskId, success_flag AS successFlag, duration_seconds AS durationSeconds, summary_text AS summaryText, metrics_json AS metricsJson, created_at AS createdAt, updated_at AS updatedAt FROM task_result_summary WHERE task_id = #{taskId} LIMIT 1")
	TaskResultSummaryPO selectByTaskId(Long taskId);

	@Insert("INSERT INTO task_result_summary(task_id, success_flag, duration_seconds, summary_text, metrics_json) VALUES(#{taskId}, #{successFlag}, #{durationSeconds}, #{summaryText}, #{metricsJson}) " +
			"ON DUPLICATE KEY UPDATE success_flag = VALUES(success_flag), duration_seconds = VALUES(duration_seconds), summary_text = VALUES(summary_text), metrics_json = VALUES(metrics_json)")
	int insertOrUpdate(TaskResultSummaryPO po);

	@Delete("DELETE FROM task_result_summary WHERE task_id = #{taskId}")
	int deleteByTaskId(Long taskId);
}
