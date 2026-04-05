package com.example.cae.task.domain.rule;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class TaskStatusRule {
	private static final Map<String, Set<String>> TRANSFER_MAP = new HashMap<>();

	static {
		TRANSFER_MAP.put("CREATED", Set.of("VALIDATED"));
		TRANSFER_MAP.put("VALIDATED", Set.of("CREATED", "QUEUED"));
		TRANSFER_MAP.put("QUEUED", Set.of("SCHEDULED", "CANCELED"));
		TRANSFER_MAP.put("SCHEDULED", Set.of("DISPATCHED", "FAILED"));
		TRANSFER_MAP.put("DISPATCHED", Set.of("RUNNING", "FAILED"));
		TRANSFER_MAP.put("RUNNING", Set.of("SUCCESS", "FAILED", "TIMEOUT", "CANCELED"));
		TRANSFER_MAP.put("FAILED", Set.of("QUEUED"));
		TRANSFER_MAP.put("TIMEOUT", Set.of("QUEUED"));
	}

	public boolean canTransfer(String fromStatus, String toStatus) {
		return TRANSFER_MAP.getOrDefault(fromStatus, Collections.emptySet()).contains(toStatus);
	}

	public boolean isFinished(String status) {
		return Set.of("SUCCESS", "FAILED", "CANCELED", "TIMEOUT").contains(status);
	}
}
