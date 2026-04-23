package com.example.cae.task.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.application.assembler.TaskResultAssembler;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskResultFile;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskResultFileRepository;
import com.example.cae.task.domain.repository.TaskResultSummaryRepository;
import com.example.cae.task.infrastructure.storage.TaskFileStorageService;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.infrastructure.support.TaskPermissionChecker;
import com.example.cae.task.interfaces.response.TaskResultFileResponse;
import com.example.cae.task.interfaces.response.TaskResultSummaryResponse;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class TaskResultAppService {
	private final TaskRepository taskRepository;
	private final TaskResultSummaryRepository taskResultSummaryRepository;
	private final TaskResultFileRepository taskResultFileRepository;
	private final TaskPermissionChecker taskPermissionChecker;
	private final TaskResultAssembler taskResultAssembler;
	private final TaskFileStorageService taskFileStorageService;
	private final TaskStoragePathSupport taskStoragePathSupport;

	public TaskResultAppService(TaskRepository taskRepository,
								TaskResultSummaryRepository taskResultSummaryRepository,
								TaskResultFileRepository taskResultFileRepository,
								TaskPermissionChecker taskPermissionChecker,
								TaskResultAssembler taskResultAssembler,
								TaskFileStorageService taskFileStorageService,
								TaskStoragePathSupport taskStoragePathSupport) {
		this.taskRepository = taskRepository;
		this.taskResultSummaryRepository = taskResultSummaryRepository;
		this.taskResultFileRepository = taskResultFileRepository;
		this.taskPermissionChecker = taskPermissionChecker;
		this.taskResultAssembler = taskResultAssembler;
		this.taskFileStorageService = taskFileStorageService;
		this.taskStoragePathSupport = taskStoragePathSupport;
	}

	public TaskResultSummaryResponse getResultSummary(Long taskId, Long userId, String roleCode) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		return taskResultSummaryRepository.findByTaskId(taskId)
				.map(taskResultAssembler::toSummaryResponse)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NOT_FOUND, "task result summary not found"));
	}

	public List<TaskResultFileResponse> getResultFiles(Long taskId, Long userId, String roleCode) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		return taskResultFileRepository.listByTaskId(taskId).stream().map(taskResultAssembler::toFileResponse).toList();
	}

	public TaskResultFile getResultFile(Long fileId, Long userId, String roleCode) {
		TaskResultFile file = taskResultFileRepository.findById(fileId).orElseThrow(() -> new BizException(ErrorCodeConstants.RESULT_FILE_NOT_FOUND, "result file not found"));
		Task task = taskRepository.findById(file.getTaskId()).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		return file;
	}

	public InputStream openResultFile(TaskResultFile file) {
		return taskFileStorageService.openFile(taskStoragePathSupport.toAbsoluteResultPath(file.getStoragePath()));
	}
}
