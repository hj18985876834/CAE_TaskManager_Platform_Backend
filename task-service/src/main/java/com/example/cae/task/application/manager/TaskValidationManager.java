package com.example.cae.task.application.manager;

import com.example.cae.common.dto.FileRuleDTO;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.domain.service.TaskValidationDomainService;
import com.example.cae.task.infrastructure.client.SolverClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskValidationManager {
	private final TaskRepository taskRepository;
	private final TaskFileRepository taskFileRepository;
	private final SolverClient solverClient;
	private final TaskStatusDomainService taskStatusDomainService;
	private final TaskValidationDomainService taskValidationDomainService;

	public TaskValidationManager(TaskRepository taskRepository, TaskFileRepository taskFileRepository, SolverClient solverClient, TaskStatusDomainService taskStatusDomainService, TaskValidationDomainService taskValidationDomainService) {
		this.taskRepository = taskRepository;
		this.taskFileRepository = taskFileRepository;
		this.solverClient = solverClient;
		this.taskStatusDomainService = taskStatusDomainService;
		this.taskValidationDomainService = taskValidationDomainService;
	}

	public void validateTask(Long taskId, Long userId) {
		Task task = loadAndCheckOwner(taskId, userId);
		List<TaskFile> files = taskFileRepository.listByTaskId(taskId);
		List<FileRuleDTO> rules = solverClient.getFileRules(task.getProfileId());

		taskValidationDomainService.checkTaskEditable(task);
		taskValidationDomainService.checkFilesMatchRules(files, rules);

		taskStatusDomainService.transfer(task, TaskStatusEnum.VALIDATED.name(), "validation passed", OperatorTypeEnum.USER.name(), userId);
		taskRepository.update(task);
	}

	private Task loadAndCheckOwner(Long taskId, Long userId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(404, "task not found"));
		if (!task.isOwner(userId)) {
			throw new BizException(403, "no permission");
		}
		return task;
	}
}

