package com.example.cae.task.infrastructure.persistence.mapper;

import com.example.cae.task.infrastructure.persistence.entity.TaskLogChunkPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskLogChunkMapper {
	@Insert("INSERT INTO task_log_chunk(task_id, seq_no, log_content) VALUES(#{taskId}, #{seqNo}, #{logContent})")
	int insert(TaskLogChunkPO po);

	@Select("SELECT id, task_id AS taskId, seq_no AS seqNo, log_content AS logContent, created_at AS createdAt FROM task_log_chunk WHERE task_id = #{taskId} AND seq_no >= #{fromSeq} ORDER BY seq_no ASC LIMIT #{pageSize}")
	List<TaskLogChunkPO> selectByTaskIdAndSeq(@Param("taskId") Long taskId, @Param("fromSeq") Integer fromSeq, @Param("pageSize") Integer pageSize);

	@Delete("DELETE FROM task_log_chunk WHERE task_id = #{taskId}")
	int deleteByTaskId(Long taskId);
}
