package com.example.cae.task.domain.repository;

import com.example.cae.task.domain.model.TaskFile;

import java.util.List;
import java.util.Optional;

public interface TaskFileRepository {
	void save(TaskFile file);

	void saveBatch(List<TaskFile> files);

	void update(TaskFile file);

	List<TaskFile> listByTaskId(Long taskId);

	Optional<TaskFile> findByTaskIdAndFileRoleAndFileKey(Long taskId, String fileRole, String fileKey);

	void deleteById(Long id);
}
