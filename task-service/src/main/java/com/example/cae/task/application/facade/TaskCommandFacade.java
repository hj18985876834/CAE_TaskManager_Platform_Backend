package com.example.cae.task.application.facade;

import com.example.cae.task.application.service.TaskCommandAppService;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.request.RetryTaskRequest;
import com.example.cae.task.interfaces.request.UpdateTaskPriorityRequest;
import com.example.cae.task.interfaces.request.UpdateTaskRequest;
import com.example.cae.task.interfaces.response.TaskActionResponse;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import com.example.cae.task.interfaces.response.TaskFileUploadResponse;
import com.example.cae.task.interfaces.response.TaskPriorityUpdateResponse;
import com.example.cae.task.interfaces.response.TaskSubmitResponse;
import com.example.cae.task.interfaces.response.TaskUpdateResponse;
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

	public TaskUpdateResponse updateTask(Long taskId, UpdateTaskRequest request, Long userId) {
		return taskCommandAppService.updateTask(taskId, request, userId);
	}

	public TaskFileUploadResponse uploadTaskFile(Long taskId, MultipartFile file, String fileKey, String fileRole, Long userId) {
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

	public TaskActionResponse cancelTask(Long taskId, Long userId, String reason) {
		return taskCommandAppService.cancelTask(taskId, userId, reason);
	}

	public TaskPriorityUpdateResponse adjustPriority(Long taskId, UpdateTaskPriorityRequest request, Long adminUserId) {
		return taskCommandAppService.adjustPriority(taskId, request, adminUserId);
	}

	public TaskActionResponse retryTask(Long taskId, RetryTaskRequest request, Long adminUserId) {
		return taskCommandAppService.retryTask(taskId, adminUserId, request == null ? null : request.getReason());
	}
}
