package com.example.cae.scheduler.infrastructure.client;

import com.example.cae.common.dto.TaskDTO;

public interface NodeAgentClient {
	void notifyDispatch(Long nodeId, TaskDTO task);
}

