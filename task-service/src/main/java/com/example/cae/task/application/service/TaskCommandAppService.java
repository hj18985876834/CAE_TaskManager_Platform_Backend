package com.example.cae.task.application.service;

import com.example.cae.task.application.manager.TaskLifecycleManager;
import com.example.cae.task.application.manager.TaskValidationManager;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.request.UpdateTaskPriorityRequest;
import com.example.cae.task.interfaces.request.UpdateTaskRequest;
import com.example.cae.task.interfaces.response.TaskActionResponse;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import com.example.cae.task.interfaces.response.TaskFileUploadResponse;
import com.example.cae.task.interfaces.response.TaskPriorityUpdateResponse;
import com.example.cae.task.interfaces.response.TaskSubmitResponse;
import com.example.cae.task.interfaces.response.TaskUpdateResponse;
import com.example.cae.task.interfaces.response.TaskValidateResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TaskCommandAppService {
	private final TaskLifecycleManager taskLifecycleManager;
	private final TaskValidationManager taskValidationManager;

	public TaskCommandAppService(TaskLifecycleManager taskLifecycleManager,
								 TaskValidationManager taskValidationManager) {
		this.taskLifecycleManager = taskLifecycleManager;
		this.taskValidationManager = taskValidationManager;
	}

	public TaskCreateResponse createTask(CreateTaskRequest request, Long userId) {
		return taskLifecycleManager.createTask(request, userId);
	}

	public TaskUpdateResponse updateTask(Long taskId, UpdateTaskRequest request, Long userId) {
		return taskLifecycleManager.updateTask(taskId, request, userId);
	}

	public TaskFileUploadResponse uploadTaskFile(Long taskId, MultipartFile file, String fileKey, String fileRole, Long userId) {
		return taskLifecycleManager.uploadTaskFile(taskId, file, fileKey, fileRole, userId);
	}

	public TaskValidateResponse validateTask(Long taskId, Long userId) {
		return taskValidationManager.validateTask(taskId, userId);
	}

	public TaskSubmitResponse submitTask(Long taskId, Long userId) {
		return taskLifecycleManager.submitTask(taskId, userId);
	}

	public void discardTask(Long taskId, Long userId, String reason) {
		taskLifecycleManager.discardTask(taskId, userId, reason);
	}

	public TaskActionResponse cancelTask(Long taskId, Long userId, String reason) {
		return taskLifecycleManager.cancelTask(taskId, userId, reason);
	}

	public TaskPriorityUpdateResponse adjustPriority(Long taskId, UpdateTaskPriorityRequest request, Long adminUserId) {
		return taskLifecycleManager.adjustPriority(taskId, request.getPriority(), adminUserId);
	}

	public TaskActionResponse retryTask(Long taskId, Long adminUserId, String reason) {
		return taskLifecycleManager.retryTask(taskId, reason, adminUserId);
	}
}
