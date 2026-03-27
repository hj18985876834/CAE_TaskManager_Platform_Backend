package com.example.cae.task.domain.service;

import com.example.cae.task.domain.model.TaskResultFile;
import com.example.cae.task.domain.model.TaskResultSummary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskResultDomainService {
	public void validateSummary(TaskResultSummary summary) {
		if (summary == null) {
			throw new IllegalArgumentException("result summary cannot be null");
		}
	}

	public void validateFiles(List<TaskResultFile> files) {
		if (files == null) {
			throw new IllegalArgumentException("result files cannot be null");
		}
	}
}
