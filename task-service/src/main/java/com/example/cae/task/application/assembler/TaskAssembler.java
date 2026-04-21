package com.example.cae.task.application.assembler;

import com.example.cae.common.constant.TaskConstants;
import com.example.cae.common.utils.JsonUtil;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.infrastructure.persistence.entity.TaskPO;
import com.example.cae.task.interfaces.request.CreateTaskRequest;
import com.example.cae.task.interfaces.response.TaskCreateResponse;
import com.example.cae.task.interfaces.response.TaskDetailResponse;
import com.example.cae.task.interfaces.response.TaskListItemResponse;
import com.example.cae.task.interfaces.response.TaskUpdateResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class TaskAssembler {
	public Task toTask(CreateTaskRequest request, Long userId) {
		Task task = new Task();
		task.setTaskName(request.getTaskName());
		task.setUserId(userId);
		task.setSolverId(request.getSolverId());
		task.setProfileId(request.getProfileId());
		task.setTaskType(request.getTaskType());
		task.setParamsJson(request.getParams() == null ? null : JsonUtil.toJson(request.getParams()));
		task.setStatus(TaskStatusEnum.CREATED.name());
		task.setPriority(request.getPriority() == null ? TaskConstants.DEFAULT_PRIORITY : request.getPriority());
		task.setDeletedFlag(0);
		task.setCreatedAt(LocalDateTime.now());
		task.setUpdatedAt(LocalDateTime.now());
		return task;
	}

	public TaskCreateResponse toCreateResponse(Task task) {
		TaskCreateResponse response = new TaskCreateResponse();
		response.setTaskId(task.getId());
		response.setTaskNo(task.getTaskNo());
		response.setStatus(task.getStatus());
		response.setPriority(task.getPriority());
		return response;
	}

	public TaskUpdateResponse toUpdateResponse(Task task) {
		TaskUpdateResponse response = new TaskUpdateResponse();
		response.setTaskId(task.getId());
		response.setStatus(task.getStatus());
		response.setPriority(task.getPriority());
		return response;
	}

	public TaskDetailResponse toDetailResponse(Task task) {
		TaskDetailResponse response = new TaskDetailResponse();
		response.setTaskId(task.getId());
		response.setTaskNo(task.getTaskNo());
		response.setTaskName(task.getTaskName());
		response.setUserId(task.getUserId());
		response.setSolverId(task.getSolverId());
		response.setProfileId(task.getProfileId());
		response.setTaskType(task.getTaskType());
		response.setStatus(task.getStatus());
		response.setPriority(task.getPriority());
		response.setNodeId(task.getNodeId());
		response.setCanCancel(canCancel(task));
		response.setCanRetry(canRetry(task));
		response.setParams(parseJsonMap(task.getParamsJson()));
		response.setFailType(task.getFailType());
		response.setFailMessage(task.getFailMessage());
		response.setSubmitTime(task.getSubmitTime());
		response.setStartTime(task.getStartTime());
		response.setEndTime(task.getEndTime());
		return response;
	}

	public TaskListItemResponse toListItemResponse(Task task) {
		TaskListItemResponse response = new TaskListItemResponse();
		response.setTaskId(task.getId());
		response.setTaskNo(task.getTaskNo());
		response.setTaskName(task.getTaskName());
		response.setSolverId(task.getSolverId());
		response.setProfileId(task.getProfileId());
		response.setTaskType(task.getTaskType());
		response.setStatus(task.getStatus());
		response.setPriority(task.getPriority());
		response.setNodeId(task.getNodeId());
		response.setCanCancel(canCancel(task));
		response.setCanRetry(canRetry(task));
		response.setSubmitTime(task.getSubmitTime());
		response.setStartTime(task.getStartTime());
		response.setEndTime(task.getEndTime());
		return response;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseJsonMap(String json) {
		if (json == null || json.isBlank()) {
			return Map.of();
		}
		try {
			Object parsed = JsonUtil.fromJson(json, Map.class);
			if (parsed instanceof Map<?, ?> map) {
				return (Map<String, Object>) map;
			}
		} catch (Exception ignored) {
			// keep query responses resilient even if stored params are malformed.
		}
		return Map.of();
	}

	private boolean canRetry(Task task) {
		if (task == null || task.getStatus() == null) {
			return false;
		}
		return TaskStatusEnum.FAILED.name().equals(task.getStatus())
				|| TaskStatusEnum.TIMEOUT.name().equals(task.getStatus());
	}

	private boolean canCancel(Task task) {
		if (task == null || task.getStatus() == null) {
			return false;
		}
		return TaskStatusEnum.QUEUED.name().equals(task.getStatus());
	}

	public TaskPO toPO(Task task) {
		TaskPO po = new TaskPO();
		po.setId(task.getId());
		po.setTaskNo(task.getTaskNo());
		po.setTaskName(task.getTaskName());
		po.setUserId(task.getUserId());
		po.setSolverId(task.getSolverId());
		po.setProfileId(task.getProfileId());
		po.setTaskType(task.getTaskType());
		po.setStatus(task.getStatus());
		po.setPriority(task.getPriority());
		po.setNodeId(task.getNodeId());
		po.setParamsJson(task.getParamsJson());
		po.setSubmitTime(task.getSubmitTime());
		po.setStartTime(task.getStartTime());
		po.setEndTime(task.getEndTime());
		po.setFailType(task.getFailType());
		po.setFailMessage(task.getFailMessage());
		po.setDeletedFlag(task.getDeletedFlag());
		return po;
	}

	public Task fromPO(TaskPO po) {
		Task task = new Task();
		task.setId(po.getId());
		task.setTaskNo(po.getTaskNo());
		task.setTaskName(po.getTaskName());
		task.setUserId(po.getUserId());
		task.setSolverId(po.getSolverId());
		task.setProfileId(po.getProfileId());
		task.setTaskType(po.getTaskType());
		task.setStatus(po.getStatus());
		task.setPriority(po.getPriority());
		task.setNodeId(po.getNodeId());
		task.setParamsJson(po.getParamsJson());
		task.setSubmitTime(po.getSubmitTime());
		task.setStartTime(po.getStartTime());
		task.setEndTime(po.getEndTime());
		task.setFailType(po.getFailType());
		task.setFailMessage(po.getFailMessage());
		task.setDeletedFlag(po.getDeletedFlag());
		task.setCreatedAt(po.getCreatedAt());
		task.setUpdatedAt(po.getUpdatedAt());
		return task;
	}
}
