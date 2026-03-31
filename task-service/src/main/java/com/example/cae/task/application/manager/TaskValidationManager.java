package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
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
import com.example.cae.task.interfaces.response.TaskValidateResponse;
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

	public TaskValidateResponse validateTask(Long taskId, Long userId) {
		Task task = loadAndCheckOwner(taskId, userId);
		List<TaskFile> files = taskFileRepository.listByTaskId(taskId);
		Long profileSolverId = solverClient.getProfileSolverId(task.getProfileId());
		String profileTaskType = solverClient.getProfileTaskType(task.getProfileId());
		List<FileRuleDTO> rules = solverClient.getFileRules(task.getProfileId());

		try {
			taskValidationDomainService.checkTaskEditable(task);
			if (profileSolverId == null) {
				throw new BizException(ErrorCodeConstants.PROFILE_NOT_FOUND, "profile not found");
			}
			if (!profileSolverId.equals(task.getSolverId())) {
				throw new BizException(ErrorCodeConstants.TASK_PROFILE_MISMATCH, "solver and profile do not match");
			}
			if (profileTaskType != null && !profileTaskType.isBlank() && !profileTaskType.equals(task.getTaskType())) {
				throw new BizException(ErrorCodeConstants.TASK_TYPE_MISMATCH, "task type and profile do not match");
			}
			taskValidationDomainService.checkFilesMatchRules(files, rules);
		} catch (BizException ex) {
			if (shouldAttachValidationData(ex)) {
				throw new BizException(ex.getCode(), ex.getMessage(), buildInvalidResponse(taskId));
			}
			throw ex;
		}

		taskStatusDomainService.transfer(task, TaskStatusEnum.VALIDATED.name(), "validation passed", OperatorTypeEnum.USER.name(), userId);
		taskRepository.update(task);
		TaskValidateResponse response = new TaskValidateResponse();
		response.setTaskId(taskId);
		response.setValid(Boolean.TRUE);
		response.setStatus(task.getStatus());
		return response;
	}

	private Task loadAndCheckOwner(Long taskId, Long userId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (!task.isOwner(userId)) {
			throw new BizException(ErrorCodeConstants.FORBIDDEN, "no permission");
		}
		return task;
	}

	private boolean shouldAttachValidationData(BizException ex) {
		return ex.getCode() != null && switch (ex.getCode()) {
			case ErrorCodeConstants.TASK_VALIDATION_FAILED,
					ErrorCodeConstants.TASK_STATUS_NOT_EDITABLE,
					ErrorCodeConstants.PROFILE_NOT_FOUND,
					ErrorCodeConstants.TASK_PROFILE_MISMATCH,
					ErrorCodeConstants.TASK_TYPE_MISMATCH -> true;
			default -> false;
		};
	}

	private TaskValidateResponse buildInvalidResponse(Long taskId) {
		TaskValidateResponse response = new TaskValidateResponse();
		response.setTaskId(taskId);
		response.setValid(Boolean.FALSE);
		return response;
	}
}
