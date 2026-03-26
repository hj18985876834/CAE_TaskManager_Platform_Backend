package com.example.cae.task.domain.repository;

import com.example.cae.task.domain.model.TaskFile;

import java.util.List;

public interface TaskFileRepository {
	void saveBatch(List<TaskFile> files);

	List<TaskFile> listByTaskId(Long taskId);
}

