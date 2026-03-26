package com.example.cae.task.domain.service;

import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskStatusHistory;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import com.example.cae.task.domain.rule.TaskStatusRule;
import org.springframework.stereotype.Service;

@Service
public class TaskStatusDomainService {
	private final TaskStatusRule taskStatusRule;
	private final TaskStatusHistoryRepository taskStatusHistoryRepository;

	public TaskStatusDomainService(TaskStatusRule taskStatusRule, TaskStatusHistoryRepository taskStatusHistoryRepository) {
		this.taskStatusRule = taskStatusRule;
		this.taskStatusHistoryRepository = taskStatusHistoryRepository;
	}

	public void transfer(Task task, String targetStatus, String reason, String operatorType, Long operatorId) {
		String fromStatus = task.getStatus();
		if (!taskStatusRule.canTransfer(fromStatus, targetStatus)) {
			throw new BizException(400, "illegal status transfer: " + fromStatus + " -> " + targetStatus);
		}

		switch (targetStatus) {
			case "VALIDATED" -> task.markValidated();
			case "QUEUED" -> task.submit();
			case "SCHEDULED" -> task.markScheduled();
			case "DISPATCHED" -> task.markDispatched();
			case "RUNNING" -> task.markRunning();
			case "SUCCESS" -> task.markSuccess();
			case "CANCELED" -> task.cancel();
			case "FAILED", "TIMEOUT" -> {
			}
			default -> throw new BizException(400, "unsupported status: " + targetStatus);
		}

		TaskStatusHistory history = new TaskStatusHistory();
		history.setTaskId(task.getId());
		history.setFromStatus(fromStatus);
		history.setToStatus(targetStatus);
		history.setChangeReason(reason);
		history.setOperatorType(operatorType);
		history.setOperatorId(operatorId);
		taskStatusHistoryRepository.save(history);
	}
}

