package com.example.cae.task.application.facade;

import com.example.cae.task.application.service.TaskCommandAppService;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
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

	public void uploadTaskFiles(Long taskId, MultipartFile[] files, Long userId) {
		taskCommandAppService.uploadTaskFiles(taskId, files, userId);
	}

	public void validateTask(Long taskId, Long userId) {
		taskCommandAppService.validateTask(taskId, userId);
	}

	public void submitTask(Long taskId, Long userId) {
		taskCommandAppService.submitTask(taskId, userId);
	}

	public void cancelTask(Long taskId, Long userId, String reason) {
		taskCommandAppService.cancelTask(taskId, userId, reason);
	}
}
