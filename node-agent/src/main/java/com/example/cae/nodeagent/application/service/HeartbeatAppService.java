package com.example.cae.nodeagent.application.service;

import com.example.cae.nodeagent.domain.model.NodeInfo;
import com.example.cae.nodeagent.infrastructure.client.SchedulerNodeClient;
import com.example.cae.nodeagent.infrastructure.support.NodeInfoCollector;
import org.springframework.stereotype.Service;

@Service
public class HeartbeatAppService {
	private final SchedulerNodeClient schedulerNodeClient;
	private final NodeInfoCollector nodeInfoCollector;

	public HeartbeatAppService(SchedulerNodeClient schedulerNodeClient, NodeInfoCollector nodeInfoCollector) {
		this.schedulerNodeClient = schedulerNodeClient;
		this.nodeInfoCollector = nodeInfoCollector;
	}

	public void sendHeartbeat() {
		NodeInfo nodeInfo = nodeInfoCollector.collectNodeInfo();
		schedulerNodeClient.heartbeat(nodeInfo);
	}
}

