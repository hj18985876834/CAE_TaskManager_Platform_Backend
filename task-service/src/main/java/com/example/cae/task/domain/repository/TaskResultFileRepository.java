package com.example.cae.task.domain.repository;

import com.example.cae.task.domain.model.TaskResultFile;

import java.util.List;
import java.util.Optional;

public interface TaskResultFileRepository {
	void save(TaskResultFile file);

	List<TaskResultFile> listByTaskId(Long taskId);

	Optional<TaskResultFile> findById(Long fileId);
}

