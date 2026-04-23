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
import com.example.cae.task.infrastructure.support.TaskPathResolver;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.infrastructure.support.TaskPermissionChecker;
import com.example.cae.task.interfaces.response.TaskResultFileResponse;
import com.example.cae.task.interfaces.response.TaskResultSummaryResponse;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
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
	private final TaskPathResolver taskPathResolver;

	public TaskResultAppService(TaskRepository taskRepository,
								TaskResultSummaryRepository taskResultSummaryRepository,
								TaskResultFileRepository taskResultFileRepository,
								TaskPermissionChecker taskPermissionChecker,
								TaskResultAssembler taskResultAssembler,
								TaskFileStorageService taskFileStorageService,
								TaskStoragePathSupport taskStoragePathSupport,
								TaskPathResolver taskPathResolver) {
		this.taskRepository = taskRepository;
		this.taskResultSummaryRepository = taskResultSummaryRepository;
		this.taskResultFileRepository = taskResultFileRepository;
		this.taskPermissionChecker = taskPermissionChecker;
		this.taskResultAssembler = taskResultAssembler;
		this.taskFileStorageService = taskFileStorageService;
		this.taskStoragePathSupport = taskStoragePathSupport;
		this.taskPathResolver = taskPathResolver;
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
		Path resultFilePath = validateDownloadableResultFile(file);
		try {
			return taskFileStorageService.openFile(resultFilePath.toString());
		} catch (RuntimeException ex) {
			throw new BizException(ErrorCodeConstants.RESULT_FILE_NOT_FOUND, "result file not found");
		}
	}

	private Path validateDownloadableResultFile(TaskResultFile file) {
		if (file == null || file.getTaskId() == null || file.getStoragePath() == null || file.getStoragePath().isBlank()) {
			throw new BizException(ErrorCodeConstants.RESULT_FILE_NOT_FOUND, "result file not found");
		}
		Path resultDir = Path.of(taskPathResolver.resolveResultDir(file.getTaskId())).toAbsolutePath().normalize();
		Path filePath;
		try {
			filePath = Path.of(taskStoragePathSupport.toAbsoluteResultPath(file.getStoragePath()))
					.toAbsolutePath()
					.normalize();
		} catch (InvalidPathException ex) {
			throw new BizException(ErrorCodeConstants.RESULT_FILE_NOT_FOUND, "result file not found");
		}
		if (!filePath.startsWith(resultDir)) {
			throw new BizException(ErrorCodeConstants.RESULT_FILE_NOT_FOUND, "result file not found");
		}
		if (filePath.getFileName() == null || file.getFileName() == null || !file.getFileName().equals(filePath.getFileName().toString())) {
			throw new BizException(ErrorCodeConstants.RESULT_FILE_NOT_FOUND, "result file not found");
		}
		if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
			throw new BizException(ErrorCodeConstants.RESULT_FILE_NOT_FOUND, "result file not found");
		}
		return filePath;
	}
}
