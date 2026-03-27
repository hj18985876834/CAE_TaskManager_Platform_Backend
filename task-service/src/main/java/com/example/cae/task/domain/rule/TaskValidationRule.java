package com.example.cae.task.domain.rule;

import com.example.cae.task.domain.model.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskValidationRule {
	public boolean canValidate(Task task) {
		return task != null && task.getStatus() != null && ("CREATED".equals(task.getStatus()) || "VALIDATION_FAILED".equals(task.getStatus()));
	}
}
