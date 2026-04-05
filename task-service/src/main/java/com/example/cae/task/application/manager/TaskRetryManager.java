package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.interfaces.response.TaskSubmitResponse;
import org.springframework.stereotype.Service;

@Service
public class TaskRetryManager {
	private final TaskRepository taskRepository;
	private final TaskStatusDomainService taskStatusDomainService;

	public TaskRetryManager(TaskRepository taskRepository, TaskStatusDomainService taskStatusDomainService) {
		this.taskRepository = taskRepository;
		this.taskStatusDomainService = taskStatusDomainService;
	}

	public TaskSubmitResponse retryTask(Long taskId, Long adminUserId, String reason) {
		Task task = taskRepository.findById(taskId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (!TaskStatusEnum.FAILED.name().equals(task.getStatus()) && !TaskStatusEnum.TIMEOUT.name().equals(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_RETRY_NOT_ALLOWED, "only failed or timeout tasks can be retried");
		}

		String effectiveReason = reason == null || reason.isBlank() ? "admin retried task" : reason;
		taskStatusDomainService.transfer(task, TaskStatusEnum.QUEUED.name(), effectiveReason, OperatorTypeEnum.ADMIN.name(), adminUserId);
		taskRepository.update(task);

		TaskSubmitResponse response = new TaskSubmitResponse();
		response.setTaskId(task.getId());
		response.setStatus(task.getStatus());
		return response;
	}
}
