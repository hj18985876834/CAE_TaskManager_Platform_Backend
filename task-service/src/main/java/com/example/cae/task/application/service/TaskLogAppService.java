package com.example.cae.task.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.application.assembler.TaskLogAssembler;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskLogChunk;
import com.example.cae.task.domain.repository.TaskLogRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.infrastructure.support.TaskPermissionChecker;
import com.example.cae.task.interfaces.response.TaskLogPageResponse;
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

	public TaskLogPageResponse getLogs(Long taskId, Integer fromSeq, Integer pageSize, Long userId, String roleCode) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		List<TaskLogResponse> records = taskLogRepository.listByTaskIdAndSeq(taskId, fromSeq, pageSize).stream().map(taskLogAssembler::toResponse).toList();
		TaskLogPageResponse response = new TaskLogPageResponse();
		response.setTaskId(taskId);
		response.setRecords(records);
		response.setNextSeq(nextSeq(records, fromSeq));
		return response;
	}

	public String getFullLogContent(Long taskId, Long userId, String roleCode) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		taskPermissionChecker.checkCanAccess(task, userId, roleCode);
		List<TaskLogChunk> records = taskLogRepository.listByTaskIdAndSeq(taskId, 0, Integer.MAX_VALUE);
		StringBuilder builder = new StringBuilder();
		for (TaskLogChunk chunk : records) {
			if (chunk.getLogContent() != null) {
				builder.append(chunk.getLogContent());
			}
		}
		return builder.toString();
	}

	private Integer nextSeq(List<TaskLogResponse> records, Integer fromSeq) {
		if (records.isEmpty()) {
			return fromSeq == null ? 0 : fromSeq;
		}
		return records.get(records.size() - 1).getSeqNo() + 1;
	}
}
