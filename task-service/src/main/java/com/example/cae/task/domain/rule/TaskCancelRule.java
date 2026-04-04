package com.example.cae.task.domain.rule;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TaskCancelRule {
	public boolean canCancel(String status) {
		return Set.of("QUEUED", "RUNNING").contains(status);
	}

	public boolean canDiscard(String status) {
		return Set.of("CREATED", "VALIDATED").contains(status);
	}
}
