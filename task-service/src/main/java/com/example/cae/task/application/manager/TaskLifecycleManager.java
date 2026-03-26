package com.example.cae.task.application.manager;

import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.application.assembler.TaskAssembler;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.domain.service.TaskValidationDomainService;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.storage.TaskFileStorageService;
import com.example.cae.task.infrastructure.support.TaskNoGenerator;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.request.StatusReportRequest;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskLifecycleManager {
	private final TaskRepository taskRepository;
	private final TaskFileRepository taskFileRepository;
	private final TaskStatusDomainService taskStatusDomainService;
	private final TaskValidationDomainService taskValidationDomainService;
	private final TaskFileStorageService taskFileStorageService;
	private final TaskAssembler taskAssembler;
	private final TaskNoGenerator taskNoGenerator;
	private final SchedulerClient schedulerClient;

	public TaskLifecycleManager(TaskRepository taskRepository, TaskFileRepository taskFileRepository, TaskStatusDomainService taskStatusDomainService, TaskValidationDomainService taskValidationDomainService, TaskFileStorageService taskFileStorageService, TaskAssembler taskAssembler, TaskNoGenerator taskNoGenerator, SchedulerClient schedulerClient) {
		this.taskRepository = taskRepository;
		this.taskFileRepository = taskFileRepository;
		this.taskStatusDomainService = taskStatusDomainService;
		this.taskValidationDomainService = taskValidationDomainService;
		this.taskFileStorageService = taskFileStorageService;
		this.taskAssembler = taskAssembler;
		this.taskNoGenerator = taskNoGenerator;
		this.schedulerClient = schedulerClient;
	}

	public TaskCreateResponse createTask(CreateTaskRequest request, Long userId) {
		Task task = taskAssembler.toTask(request, userId);
		task.setTaskNo(taskNoGenerator.generateTaskNo());
		task.setStatus(TaskStatusEnum.CREATED.name());
		taskRepository.save(task);
		return taskAssembler.toCreateResponse(task);
	}

	public void uploadTaskFiles(Long taskId, MultipartFile[] files, Long userId) {
		Task task = loadAndCheckOwner(taskId, userId);
		taskValidationDomainService.checkTaskEditable(task);
		List<TaskFile> taskFiles = new ArrayList<>();
		for (MultipartFile file : files) {
			taskFiles.add(taskFileStorageService.saveInputFile(taskId, file));
		}
		taskFileRepository.saveBatch(taskFiles);
	}

	public void submitTask(Long taskId, Long userId) {
		Task task = loadAndCheckOwner(taskId, userId);
		taskValidationDomainService.checkTaskCanSubmit(task);
		taskStatusDomainService.transfer(task, TaskStatusEnum.QUEUED.name(), "task submitted", OperatorTypeEnum.USER.name(), userId);
		taskRepository.update(task);
		schedulerClient.notifyTaskSubmitted(taskId);
	}

	public void cancelTask(Long taskId, Long userId, String reason) {
		Task task = loadAndCheckOwner(taskId, userId);
		taskStatusDomainService.transfer(task, TaskStatusEnum.CANCELED.name(), reason, OperatorTypeEnum.USER.name(), userId);
		taskRepository.update(task);
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

	public void reportStatus(Long taskId, StatusReportRequest request) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(404, "task not found"));
		taskStatusDomainService.transfer(task, request.getStatus(), request.getReason(), OperatorTypeEnum.NODE.name(), null);
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

