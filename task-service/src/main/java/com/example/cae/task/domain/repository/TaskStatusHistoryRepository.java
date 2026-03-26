package com.example.cae.task.domain.repository;

import com.example.cae.task.domain.model.TaskStatusHistory;

import java.util.List;

public interface TaskStatusHistoryRepository {
	void save(TaskStatusHistory history);

	List<TaskStatusHistory> listByTaskId(Long taskId);
}

