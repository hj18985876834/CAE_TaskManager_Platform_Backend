package com.example.cae.task.domain.service;

import com.example.cae.common.constant.ErrorCodeConstants;
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
		String normalizedTargetStatus = targetStatus == null ? null : targetStatus.trim().toUpperCase();
		if (!taskStatusRule.canTransfer(fromStatus, normalizedTargetStatus)) {
			throw new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status transfer: " + fromStatus + " -> " + targetStatus);
		}

		switch (normalizedTargetStatus) {
			case "VALIDATED" -> task.markValidated();
			case "QUEUED" -> task.submit();
			case "SCHEDULED" -> task.markScheduled();
			case "DISPATCHED" -> task.markDispatched();
			case "RUNNING" -> task.markRunning();
			case "SUCCESS" -> task.markSuccess();
			case "CANCELED" -> task.cancel();
			case "FAILED" -> task.markFailed(task.getFailType(), reason);
			case "TIMEOUT" -> task.markTimeout(reason);
			default -> throw new BizException(ErrorCodeConstants.TASK_STATUS_UNSUPPORTED, "unsupported status: " + targetStatus);
		}

		TaskStatusHistory history = new TaskStatusHistory();
		history.setTaskId(task.getId());
		history.setFromStatus(fromStatus);
		history.setToStatus(normalizedTargetStatus);
		history.setChangeReason(reason);
		history.setOperatorType(operatorType);
		history.setOperatorId(operatorId);
		taskStatusHistoryRepository.save(history);
	}

	public void recordInitialStatus(Task task, String reason, String operatorType, Long operatorId) {
		TaskStatusHistory history = new TaskStatusHistory();
		history.setTaskId(task.getId());
		history.setFromStatus(null);
		history.setToStatus(task.getStatus());
		history.setChangeReason(reason);
		history.setOperatorType(operatorType);
		history.setOperatorId(operatorId);
		taskStatusHistoryRepository.save(history);
	}
}
