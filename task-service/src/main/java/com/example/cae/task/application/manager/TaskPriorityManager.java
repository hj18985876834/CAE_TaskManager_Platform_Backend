package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskStatusHistory;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class TaskPriorityManager {
	private final TaskRepository taskRepository;
	private final TaskStatusHistoryRepository taskStatusHistoryRepository;

	public TaskPriorityManager(TaskRepository taskRepository, TaskStatusHistoryRepository taskStatusHistoryRepository) {
		this.taskRepository = taskRepository;
		this.taskStatusHistoryRepository = taskStatusHistoryRepository;
	}

	public void adjustPriority(Long taskId, Integer priority, Long adminUserId) {
		Task task = taskRepository.findById(taskId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (task.isFinished()) {
			throw new BizException(ErrorCodeConstants.TASK_PRIORITY_UPDATE_NOT_ALLOWED, "finished task priority cannot be adjusted");
		}

		Integer oldPriority = task.getPriority() == null ? 0 : task.getPriority();
		Integer newPriority = priority == null ? 0 : priority;
		task.adjustPriority(newPriority);
		taskRepository.update(task);

		TaskStatusHistory history = new TaskStatusHistory();
		history.setTaskId(task.getId());
		history.setFromStatus(task.getStatus());
		history.setToStatus(task.getStatus());
		history.setChangeReason("priority adjusted: " + oldPriority + " -> " + newPriority);
		history.setOperatorType(OperatorTypeEnum.ADMIN.name());
		history.setOperatorId(adminUserId);
		taskStatusHistoryRepository.save(history);
	}
}
