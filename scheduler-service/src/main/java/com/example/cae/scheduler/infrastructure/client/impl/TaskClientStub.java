package com.example.cae.scheduler.infrastructure.client.impl;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Primary
public class TaskClientStub implements TaskClient {
	@Override
	public List<TaskDTO> listPendingTasks() {
		return Collections.emptyList();
	}

	@Override
	public void markTaskScheduled(Long taskId, Long nodeId) {
		// Structural placeholder for cross-service integration in later stage.
	}
}
