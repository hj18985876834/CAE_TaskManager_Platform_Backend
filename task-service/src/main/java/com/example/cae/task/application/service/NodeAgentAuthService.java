package com.example.cae.task.application.service;

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
		if (taskId == null) {
			throw new BizException(400, "taskId is required");
		}
		if (nodeToken == null || nodeToken.isBlank()) {
			throw new BizException(401, "node token required");
		}
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(404, "task not found"));
		if (task.getNodeId() == null) {
			throw new BizException(409, "task not bound to node");
		}
		boolean valid = schedulerClient.verifyNodeToken(task.getNodeId(), nodeToken);
		if (!valid) {
			throw new BizException(401, "invalid node token");
		}
	}
}
