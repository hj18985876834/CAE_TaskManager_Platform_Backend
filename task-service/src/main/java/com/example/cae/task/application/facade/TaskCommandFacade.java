package com.example.cae.task.application.facade;

import com.example.cae.task.application.service.TaskCommandAppService;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.request.UpdateTaskPriorityRequest;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import com.example.cae.task.interfaces.response.TaskFileResponse;
import com.example.cae.task.interfaces.response.TaskSubmitResponse;
import com.example.cae.task.interfaces.response.TaskValidateResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class TaskCommandFacade {
	private final TaskCommandAppService taskCommandAppService;

	public TaskCommandFacade(TaskCommandAppService taskCommandAppService) {
		this.taskCommandAppService = taskCommandAppService;
	}

	public TaskCreateResponse createTask(CreateTaskRequest request, Long userId) {
		return taskCommandAppService.createTask(request, userId);
	}

	public TaskFileResponse uploadTaskFile(Long taskId, MultipartFile file, String fileKey, String fileRole, Long userId) {
		return taskCommandAppService.uploadTaskFile(taskId, file, fileKey, fileRole, userId);
	}

	public TaskValidateResponse validateTask(Long taskId, Long userId) {
		return taskCommandAppService.validateTask(taskId, userId);
	}

	public TaskSubmitResponse submitTask(Long taskId, Long userId) {
		return taskCommandAppService.submitTask(taskId, userId);
	}

	public void discardTask(Long taskId, Long userId, String reason) {
		taskCommandAppService.discardTask(taskId, userId, reason);
	}

	public void cancelTask(Long taskId, Long userId, String reason) {
		taskCommandAppService.cancelTask(taskId, userId, reason);
	}

	public void adjustPriority(Long taskId, UpdateTaskPriorityRequest request, Long adminUserId) {
		taskCommandAppService.adjustPriority(taskId, request, adminUserId);
	}
}
