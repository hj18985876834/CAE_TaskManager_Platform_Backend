package com.example.cae.task.domain.service;

import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.rule.TaskCancelRule;
import org.springframework.stereotype.Service;

@Service
public class TaskDomainService {
	private final TaskCancelRule taskCancelRule;

	public TaskDomainService(TaskCancelRule taskCancelRule) {
		this.taskCancelRule = taskCancelRule;
	}

	public boolean canCancel(Task task) {
		return task != null && taskCancelRule.canCancel(task.getStatus());
	}
}
