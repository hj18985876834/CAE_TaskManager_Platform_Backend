package com.example.cae.task.domain.repository;

import com.example.cae.task.domain.model.TaskResultSummary;

import java.util.Optional;

public interface TaskResultSummaryRepository {
	void saveOrUpdate(TaskResultSummary summary);

	Optional<TaskResultSummary> findByTaskId(Long taskId);
}

