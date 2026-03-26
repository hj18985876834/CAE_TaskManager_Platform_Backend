package com.example.cae.scheduler.infrastructure.client;

public interface NodeAgentClient {
	void notifyDispatch(Long nodeId, Long taskId);
}

