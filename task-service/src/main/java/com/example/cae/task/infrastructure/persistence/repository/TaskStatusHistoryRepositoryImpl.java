package com.example.cae.task.infrastructure.persistence.repository;

import com.example.cae.task.domain.model.TaskStatusHistory;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import com.example.cae.task.infrastructure.persistence.entity.TaskStatusHistoryPO;
import com.example.cae.task.infrastructure.persistence.mapper.TaskStatusHistoryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TaskStatusHistoryRepositoryImpl implements TaskStatusHistoryRepository {
	private final TaskStatusHistoryMapper taskStatusHistoryMapper;

	public TaskStatusHistoryRepositoryImpl(TaskStatusHistoryMapper taskStatusHistoryMapper) {
		this.taskStatusHistoryMapper = taskStatusHistoryMapper;
	}

	@Override
	public void save(TaskStatusHistory history) {
		TaskStatusHistoryPO po = new TaskStatusHistoryPO();
		po.setTaskId(history.getTaskId());
		po.setFromStatus(history.getFromStatus());
		po.setToStatus(history.getToStatus());
		po.setChangeReason(history.getChangeReason());
		po.setOperatorType(history.getOperatorType());
		po.setOperatorId(history.getOperatorId());
		taskStatusHistoryMapper.insert(po);
	}

	@Override
	public List<TaskStatusHistory> listByTaskId(Long taskId) {
		return taskStatusHistoryMapper.selectByTaskId(taskId).stream().map(this::toDomain).toList();
	}

	private TaskStatusHistory toDomain(TaskStatusHistoryPO po) {
		TaskStatusHistory history = new TaskStatusHistory();
		history.setId(po.getId());
		history.setTaskId(po.getTaskId());
		history.setFromStatus(po.getFromStatus());
		history.setToStatus(po.getToStatus());
		history.setChangeReason(po.getChangeReason());
		history.setOperatorType(po.getOperatorType());
		history.setOperatorId(po.getOperatorId());
		history.setCreatedAt(po.getCreatedAt());
		return history;
	}
}

