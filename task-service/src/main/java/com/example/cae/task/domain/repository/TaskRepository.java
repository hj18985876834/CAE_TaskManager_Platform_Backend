package com.example.cae.task.domain.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.interfaces.request.TaskListQueryRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {
	Optional<Task> findById(Long taskId);

	Optional<Task> findByIdForUpdate(Long taskId);

	void save(Task task);

	void update(Task task);

	PageResult<Task> pageMyTasks(TaskListQueryRequest request, Long userId);

	PageResult<Task> pageAdminTasks(TaskListQueryRequest request);

	List<Task> listByStatus(String status);

	List<Task> listByNodeIdAndStatuses(Long nodeId, List<String> statuses);

	long countAll();

	long countByStatus(String status);

	long countFinished();

	List<Task> listStaleUnsubmittedTasks(LocalDateTime updatedBefore, int limit);
}
