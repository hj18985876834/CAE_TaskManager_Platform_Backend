package com.example.cae.task.application.manager;

import com.example.cae.common.dto.TaskFileDTO;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.utils.JsonUtil;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.infrastructure.client.SolverClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TaskDispatchManager {
	private final TaskRepository taskRepository;
	private final TaskFileRepository taskFileRepository;
	private final TaskStatusDomainService taskStatusDomainService;
	private final SolverClient solverClient;

	public TaskDispatchManager(TaskRepository taskRepository,
						   TaskFileRepository taskFileRepository,
						   TaskStatusDomainService taskStatusDomainService,
						   SolverClient solverClient) {
		this.taskRepository = taskRepository;
		this.taskFileRepository = taskFileRepository;
		this.taskStatusDomainService = taskStatusDomainService;
		this.solverClient = solverClient;
	}

	public List<TaskDTO> listQueuedTasks() {
		return taskRepository.listByStatus(TaskStatusEnum.QUEUED.name()).stream().map(this::toTaskDTO).toList();
	}

	public List<TaskDTO> listQueuedTasks(Integer limit) {
		return taskRepository.listByStatus(TaskStatusEnum.QUEUED.name()).stream()
				.limit(limit == null || limit < 1 ? Long.MAX_VALUE : limit.longValue())
				.map(this::toTaskDTO)
				.toList();
	}

	public void markScheduled(Long taskId, Long nodeId) {
		if (nodeId == null) {
			throw new BizException(400, "nodeId is required");
		}
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(404, "task not found"));
		task.bindNode(nodeId);
		taskStatusDomainService.transfer(task, TaskStatusEnum.SCHEDULED.name(), "scheduler selected node", OperatorTypeEnum.SYSTEM.name(), null);
		taskRepository.update(task);
	}

	public void markDispatched(Long taskId, Long nodeId) {
		if (nodeId == null) {
			throw new BizException(400, "nodeId is required");
		}
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(404, "task not found"));
		task.bindNode(nodeId);
		taskStatusDomainService.transfer(task, TaskStatusEnum.DISPATCHED.name(), "task dispatched", OperatorTypeEnum.SYSTEM.name(), null);
		taskRepository.update(task);
	}

	private TaskDTO toTaskDTO(Task task) {
		TaskDTO dto = new TaskDTO();
		dto.setTaskId(task.getId());
		dto.setTaskNo(task.getTaskNo());
		dto.setTaskName(task.getTaskName());
		dto.setSolverId(task.getSolverId());
		dto.setProfileId(task.getProfileId());
		dto.setTaskType(task.getTaskType());
		dto.setParamsJson(task.getParamsJson());
		dto.setParams(parseParams(task.getParamsJson()));
		dto.setInputFiles(loadInputFiles(task.getId()));
		enrichExecutionMeta(task, dto);
		dto.setNodeId(task.getNodeId());
		return dto;
	}

	private void enrichExecutionMeta(Task task, TaskDTO dto) {
		try {
			String solverCode = solverClient.getSolverCode(task.getSolverId());
			if (solverCode != null && !solverCode.isBlank()) {
				dto.setSolverCode(solverCode);
			}
		} catch (Exception ignored) {
			// keep queued API resilient even if solver-service is temporarily unavailable.
		}

		try {
			SolverClient.ProfileExecutionMeta meta = solverClient.getProfileExecutionMeta(task.getProfileId());
			if (meta != null) {
				if (meta.getCommandTemplate() != null && !meta.getCommandTemplate().isBlank()) {
					dto.setCommandTemplate(meta.getCommandTemplate());
				}
				if (meta.getParserName() != null && !meta.getParserName().isBlank()) {
					dto.setParserName(meta.getParserName());
				}
				if (meta.getTimeoutSeconds() != null && meta.getTimeoutSeconds() > 0) {
					dto.setTimeoutSeconds(meta.getTimeoutSeconds());
				}
			}
		} catch (Exception ignored) {
			// keep queued API resilient even if solver-service is temporarily unavailable.
		}
	}

	private List<TaskFileDTO> loadInputFiles(Long taskId) {
		return taskFileRepository.listByTaskId(taskId).stream()
				.filter(TaskFile::isInputFile)
				.map(this::toTaskFileDTO)
				.toList();
	}

	private TaskFileDTO toTaskFileDTO(TaskFile file) {
		TaskFileDTO dto = new TaskFileDTO();
		dto.setTaskId(file.getTaskId());
		dto.setFileKey(file.getFileKey());
		dto.setOriginName(file.getOriginName());
		dto.setStoragePath(file.getStoragePath());
		dto.setFileSize(file.getFileSize());
		return dto;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseParams(String paramsJson) {
		if (paramsJson == null || paramsJson.isBlank()) {
			return Map.of();
		}
		try {
			Object parsed = JsonUtil.fromJson(paramsJson, Map.class);
			if (parsed instanceof Map<?, ?> map) {
				return (Map<String, Object>) map;
			}
		} catch (Exception ignored) {
			// keep dispatch payload resilient when params are malformed.
		}
		return Map.of();
	}
}
