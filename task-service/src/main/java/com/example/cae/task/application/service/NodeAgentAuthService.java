package com.example.cae.task.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import org.springframework.stereotype.Service;

@Service
public class NodeAgentAuthService {
	private final TaskRepository taskRepository;
	private final SchedulerClient schedulerClient;

	public NodeAgentAuthService(TaskRepository taskRepository, SchedulerClient schedulerClient) {
		this.taskRepository = taskRepository;
		this.schedulerClient = schedulerClient;
	}

	public void validateTaskNodeToken(Long taskId, String nodeToken) {
		validateTaskNodeToken(taskId, null, nodeToken);
	}

	public void validateTaskNodeToken(Long taskId, Long reportedNodeId, String nodeToken) {
		if (taskId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "taskId is required");
		}
		if (nodeToken == null || nodeToken.isBlank()) {
			throw new BizException(ErrorCodeConstants.NODE_TOKEN_REQUIRED, "node token required");
		}
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (task.getNodeId() == null) {
			throw new BizException(ErrorCodeConstants.TASK_NOT_BOUND_TO_NODE, "task not bound to node");
		}
		if (reportedNodeId != null && !reportedNodeId.equals(task.getNodeId())) {
			throw new BizException(ErrorCodeConstants.REPORTED_NODE_MISMATCH, "reported node mismatch");
		}
		boolean valid = schedulerClient.verifyNodeToken(task.getNodeId(), nodeToken);
		if (!valid) {
			throw new BizException(ErrorCodeConstants.INVALID_NODE_TOKEN, "invalid node token");
		}
	}
}
