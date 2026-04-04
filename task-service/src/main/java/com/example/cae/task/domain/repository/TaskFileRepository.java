package com.example.cae.task.domain.repository;

import com.example.cae.task.domain.model.TaskFile;

import java.util.List;

public interface TaskFileRepository {
	void save(TaskFile file);

	void saveBatch(List<TaskFile> files);

	void update(TaskFile file);

	List<TaskFile> listByTaskId(Long taskId);
}
