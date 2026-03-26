package com.example.cae.task.application.service;

import com.example.cae.task.application.manager.TaskLifecycleManager;
import com.example.cae.task.application.manager.TaskValidationManager;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TaskCommandAppService {
	private final TaskLifecycleManager taskLifecycleManager;
	private final TaskValidationManager taskValidationManager;

	public TaskCommandAppService(TaskLifecycleManager taskLifecycleManager, TaskValidationManager taskValidationManager) {
		this.taskLifecycleManager = taskLifecycleManager;
		this.taskValidationManager = taskValidationManager;
	}

	public TaskCreateResponse createTask(CreateTaskRequest request, Long userId) {
		return taskLifecycleManager.createTask(request, userId);
	}

	public void uploadTaskFiles(Long taskId, MultipartFile[] files, Long userId) {
		taskLifecycleManager.uploadTaskFiles(taskId, files, userId);
	}

	public void validateTask(Long taskId, Long userId) {
		taskValidationManager.validateTask(taskId, userId);
	}

	public void submitTask(Long taskId, Long userId) {
		taskLifecycleManager.submitTask(taskId, userId);
	}

	public void cancelTask(Long taskId, Long userId, String reason) {
		taskLifecycleManager.cancelTask(taskId, userId, reason);
	}
}

