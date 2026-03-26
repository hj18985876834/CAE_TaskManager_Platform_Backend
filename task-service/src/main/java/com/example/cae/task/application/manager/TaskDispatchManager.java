package com.example.cae.task.application.manager;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskDispatchManager {
	private final TaskRepository taskRepository;
	private final TaskStatusDomainService taskStatusDomainService;

	public TaskDispatchManager(TaskRepository taskRepository, TaskStatusDomainService taskStatusDomainService) {
		this.taskRepository = taskRepository;
		this.taskStatusDomainService = taskStatusDomainService;
	}

	public List<TaskDTO> listQueuedTasks() {
		return taskRepository.listByStatus(TaskStatusEnum.QUEUED.name()).stream().map(this::toTaskDTO).toList();
	}

	public void markScheduled(Long taskId, Long nodeId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(404, "task not found"));
		task.bindNode(nodeId);
		taskStatusDomainService.transfer(task, TaskStatusEnum.SCHEDULED.name(), "scheduler selected node", OperatorTypeEnum.SYSTEM.name(), null);
		taskRepository.update(task);
	}

	public void markDispatched(Long taskId, Long nodeId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(404, "task not found"));
		task.bindNode(nodeId);
		taskStatusDomainService.transfer(task, TaskStatusEnum.DISPATCHED.name(), "task dispatched", OperatorTypeEnum.SYSTEM.name(), null);
		taskRepository.update(task);
	}

	private TaskDTO toTaskDTO(Task task) {
		TaskDTO dto = new TaskDTO();
		dto.setTaskId(task.getId());
		dto.setTaskNo(task.getTaskNo());
		dto.setTaskName(task.getTaskName());
		dto.setSolverId(task.getSolverId());
		dto.setProfileId(task.getProfileId());
		dto.setTaskType(task.getTaskType());
		dto.setParamsJson(task.getParamsJson());
		dto.setNodeId(task.getNodeId());
		return dto;
	}
}

