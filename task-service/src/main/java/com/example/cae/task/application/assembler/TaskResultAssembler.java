package com.example.cae.task.application.assembler;

import com.example.cae.task.domain.model.TaskResultFile;
import com.example.cae.task.domain.model.TaskResultSummary;
import com.example.cae.task.interfaces.response.TaskResultFileResponse;
import com.example.cae.task.interfaces.response.TaskResultSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskResultAssembler {
	public TaskResultSummaryResponse toSummaryResponse(TaskResultSummary summary) {
		TaskResultSummaryResponse response = new TaskResultSummaryResponse();
		response.setId(summary.getId());
		response.setTaskId(summary.getTaskId());
		response.setSuccessFlag(summary.getSuccessFlag());
		response.setDurationSeconds(summary.getDurationSeconds());
		response.setSummaryText(summary.getSummaryText());
		response.setMetricsJson(summary.getMetricsJson());
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
		response.setStoragePath(file.getStoragePath());
		response.setFileSize(file.getFileSize());
		response.setCreatedAt(file.getCreatedAt());
		return response;
	}
}

