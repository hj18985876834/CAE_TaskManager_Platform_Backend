package com.example.cae.nodeagent.infrastructure.client;

import com.example.cae.nodeagent.domain.model.NodeInfo;

public interface SchedulerNodeClient {
	void register(NodeInfo nodeInfo);

	void heartbeat(NodeInfo nodeInfo);

	void updateRunningCount(Integer delta);
}
