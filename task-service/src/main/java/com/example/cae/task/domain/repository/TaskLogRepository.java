package com.example.cae.task.domain.repository;

import com.example.cae.task.domain.model.TaskLogChunk;

import java.util.List;

public interface TaskLogRepository {
	void save(TaskLogChunk chunk);

	List<TaskLogChunk> listByTaskIdAndSeq(Long taskId, Integer fromSeq, Integer pageSize);
}

