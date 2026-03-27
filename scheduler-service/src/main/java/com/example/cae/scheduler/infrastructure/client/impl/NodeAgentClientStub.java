package com.example.cae.scheduler.infrastructure.client.impl;

import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class NodeAgentClientStub implements NodeAgentClient {
	@Override
	public void notifyDispatch(Long nodeId, Long taskId) {
		// Structural placeholder for node-agent dispatch integration.
	}
}
