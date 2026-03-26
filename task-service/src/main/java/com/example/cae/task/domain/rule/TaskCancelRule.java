package com.example.cae.task.domain.rule;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TaskCancelRule {
	public boolean canCancel(String status) {
		return Set.of("CREATED", "VALIDATED", "QUEUED", "RUNNING").contains(status);
	}
}

