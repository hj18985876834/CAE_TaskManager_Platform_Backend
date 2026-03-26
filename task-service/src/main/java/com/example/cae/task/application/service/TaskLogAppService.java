package com.example.cae.task.application.service;

import com.example.cae.common.exception.BizException;
import com.example.cae.task.application.assembler.TaskLogAssembler;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.repository.TaskLogRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.infrastructure.support.TaskPermissionChecker;
import com.example.cae.task.interfaces.response.TaskLogResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskLogAppService {
	private final TaskLogRepository taskLogRepository;
	private final TaskRepository taskRepository;
	private final TaskPermissionChecker taskPermissionChecker;
	private final TaskLogAssembler taskLogAssembler;

	public TaskLogAppService(TaskLogRepository taskLogRepository, TaskRepository taskRepository, TaskPermissionChecker taskPermissionChecker, TaskLogAssembler taskLogAssembler) {
		this.taskLogRepository = taskLogRepository;
		this.taskRepository = taskRepository;
		this.taskPermissionChecker = taskPermissionChecker;
		this.taskLogAssembler = taskLogAssembler;
	}

	public List<TaskLogResponse> getLogs(Long taskId, Integer fromSeq, Integer pageSize, Long userId, String roleCode) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(404, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		return taskLogRepository.listByTaskIdAndSeq(taskId, fromSeq, pageSize).stream().map(taskLogAssembler::toResponse).toList();
	}
}

