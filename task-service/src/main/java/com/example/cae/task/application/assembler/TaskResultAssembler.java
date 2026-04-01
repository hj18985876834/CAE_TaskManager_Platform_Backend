package com.example.cae.task.application.assembler;

import com.example.cae.common.utils.JsonUtil;
import com.example.cae.task.domain.model.TaskResultFile;
import com.example.cae.task.domain.model.TaskResultSummary;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.response.TaskResultFileResponse;
import com.example.cae.task.interfaces.response.TaskResultSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TaskResultAssembler {
	private final TaskStoragePathSupport taskStoragePathSupport;

	public TaskResultAssembler(TaskStoragePathSupport taskStoragePathSupport) {
		this.taskStoragePathSupport = taskStoragePathSupport;
	}

	public TaskResultSummaryResponse toSummaryResponse(TaskResultSummary summary) {
		TaskResultSummaryResponse response = new TaskResultSummaryResponse();
		response.setId(summary.getId());
		response.setTaskId(summary.getTaskId());
		response.setSuccessFlag(summary.getSuccessFlag());
		response.setDurationSeconds(summary.getDurationSeconds());
		response.setSummaryText(summary.getSummaryText());
		response.setMetrics(parseMetrics(summary.getMetricsJson()));
		response.setCreatedAt(summary.getCreatedAt());
		response.setUpdatedAt(summary.getUpdatedAt());
		return response;
	}

	public TaskResultFileResponse toFileResponse(TaskResultFile file) {
		TaskResultFileResponse response = new TaskResultFileResponse();
		response.setId(file.getId());
		response.setTaskId(file.getTaskId());
		response.setFileType(file.getFileType());
		response.setFileName(file.getFileName());
		String displayPath = taskStoragePathSupport.toDisplayResultPath(file.getStoragePath());
		response.setStoragePath(displayPath);
		response.setRelativePath(displayPath);
		response.setDownloadUrl("/api/tasks/result-files/" + file.getId() + "/download");
		response.setFileSize(file.getFileSize());
		response.setCreatedAt(file.getCreatedAt());
		return response;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseMetrics(String metricsJson) {
		if (metricsJson == null || metricsJson.isBlank()) {
			return Map.of();
		}
		try {
			Object parsed = JsonUtil.fromJson(metricsJson, Map.class);
			if (parsed instanceof Map<?, ?> map) {
				return (Map<String, Object>) map;
			}
		} catch (Exception ignored) {
			// keep result query resilient even if stored metrics are malformed.
		}
		return Map.of();
	}
}
